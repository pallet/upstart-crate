(ns pallet.crate.upstart
  "Create upstart daemon scripts"
  (:require
   [clojure.string :as string]
   [clojure.tools.logging :refer [warnf debugf]]
   [pallet.action :refer [with-action-options]]
   [pallet.actions
    :refer [directory exec-checked-script package plan-when plan-when-not
            remote-file]]
   [pallet.actions-impl :refer [service-script-path]]
   [pallet.api :refer [plan-fn] :as api]
   [pallet.crate :refer [admin-user assoc-settings defmethod-plan defplan
                         get-settings target-flag? update-settings]]
   [pallet.crate.service
    :refer [service-supervisor service-supervisor-available?
            service-supervisor-config]]
   [pallet.crate-install :as crate-install]
   [pallet.script.lib
    :refer [cat chown cp exit file make-temp-file pkg-sbin rm state-root sudo
            upstart-script-dir]]
   [pallet.stevedore :refer [fragment script]]
   [pallet.version-dispatch :refer [defmethod-version-plan
                                    defmulti-version-plan]]))

;;; # Settings
(defn default-settings
  "Provides default settings, that are merged with any user supplied settings."
  []
  ;; TODO add configuration options here
  {:service-dir (fragment (upstart-script-dir))
   :sbin-dir (fragment (pkg-sbin))
   :bin-dir "/bin"
   :verify-dir (fragment (file (state-root) "pallet" "pallet-bin"))})

;;; ## Settings
(defmulti-version-plan settings-map [version settings])

(defmethod-version-plan
    settings-map {:os :linux}
    [os os-version version settings]
  (cond
   (:install-strategy settings) settings
   :else (assoc settings
           :install-strategy :packages
           :packages ["upstart"])))

(defplan settings
  "Settings for upstart"
  [{:keys [instance-id] :as settings}]
  (let [settings (merge (default-settings) settings)
        settings (settings-map (:version settings) settings)]
    (assoc-settings :upstart settings {:instance-id instance-id})))

;;; # Install
(defplan install
  "Install upstart"
  [{:keys [instance-id]}]
  (let [settings (get-settings :upstart {:instance-id instance-id})]
    (crate-install/install :upstart instance-id)))

;;; ## Configuration DSL
(def names {:pre-start-exec "pre-start exec"
            :post-start-exec "post-start exec"
            :pre-stop-exec "pre-stop exec"
            :post-stop-exec "post-stop exec"
            :pre-start-script "pre-start script"
            :post-start-script "post-start script"
            :pre-stop-script "pre-stop script"
            :post-stop-script "post-stop script"})

(defn- name-for [k]
  (k names (string/replace (name k) "-" " ")))

(defmulti format-stanza
  "Format an upstart stanza"
  (fn [[k v]]
    (cond
      (#{:exec
         :pre-start-exec :post-start-exec :pre-stop-exec :post-stop-exec
         :start-on :stop-on
         :respawn-limit :normal-exit
         :instance
         :version :emits
         :console :umask :nice :oom :chroot :chdir
         :kill-timeout
         :setuid} k) :simple
      (#{:description :author} k) :quoted-string
      (#{:respawn} k) :boolean
      (#{:script :pre-start-script :post-start-script :pre-stop-script
         :post-stop-script} k) :block
      (#{:env :export :kill-timeout :expect} k) :multi
      (#{:limit} k) :map
      :else :simple)))

(defmethod format-stanza :simple
  [[k v]]
  (format "%s %s" (name-for k) v))

(defmethod format-stanza :quoted-string
  [[k v]]
  (format "%s \"%s\"" (name-for k) v))

(defmethod format-stanza :multi
  [[k v]]
  (if (sequential? v)
    (string/join "\n" (map #(format "%s %s" (name-for k) %) v))
    (format "%s %s" (name-for k) v)))

(defmethod format-stanza :map
  [[k v]]
  (string/join
   "\n"
   (map #(format "%s %s %s" (name-for k) (first %) (second %)) v)))

(defmethod format-stanza :boolean
  [[k v]]
  (if v
    (format "%s" (name-for k))))

(defmethod format-stanza :block
  [[k v]]
  (if v
    (format
     "%s\n%s\nend %s"
     (name-for k)
     v
     (last (string/split (name-for k) #" ")))))

(defn- job-format [options]
  (string/join \newline (concat (map format-stanza options))))

(defn job
  "Define an upstart job.
    :start-on, :stop-on, :env, :export takes a sequency of strings.
    :limit takes a map of limit-resource and soft hard limit pairs as a string"
  [service-name
   {:keys [script exec
           pre-start-script post-start-script pre-stop-script post-stop-script
           pre-start-exec post-start-exec pre-stop-exec post-stop-exec
           start-on stop-on
           env export respawn respawn-limit normal-exit
           instance
           description author version emits
           console umask nice oom chroot chdir limit
           kill-timeout expect] :as service-options}
   {:keys [instance-id] :as options}]
  (update-settings
   :upstart options assoc-in [:jobs (keyword service-name)] service-options))

;;; # Service Supervisor Implementation
(defmethod service-supervisor-available? :upstart
  [_]
  true)

(defmethod service-supervisor-config :upstart
  [_
   {:keys [service-name run-file script exec
           pre-start-script post-start-script pre-stop-script post-stop-script
           pre-start-exec post-start-exec pre-stop-exec post-stop-exec
           start-on stop-on
           env export respawn respawn-limit normal-exit
           instance
           description author version emits
           console umask nice oom chroot chdir limit
           kill-timeout expect] :as service-options}
   {:keys [instance-id] :as options}]
  (job service-name (dissoc service-options :service-name) options))


(defn verify-conf-script
  "Return a script to verify an upstart job."
  ;; https://gist.github.com/whiteley/4256487
  ;; http://scriptogr.am/mwhiteley/post/dbus-init-checkconf
  [bin-dir]
  (fragment
   ;; start a bus
   (set! dbus_pid_file (make-temp-file "dbus"))
   ("exec" "4<>" @dbus_pid_file)
   (set! dbus_add_file (make-temp-file "dbus"))
   ("exec" "6<>" @dbus_add_file)
   ("/bin/dbus-daemon" "--fork"
    "--print-pid" 4 "--print-address" 6 "--session")
   ;; function to stop the bus
   (defn clean []
     ("kill" @(cat @dbus_pid_file))
     (rm @dbus_pid_file :force true)
     (rm @dbus_add_file :force true)
     (exit @rv))
   ("trap" (quoted "{ clean; }") "EXIT")

   ;; verify config
   (println "Verifying" @1)
   (set! tmpfile (str (make-temp-file "verify") ".conf"))
   (cp @1 @tmpfile)
   (chown ~(:username (admin-user)) @tmpfile)
   ("export" (set! DBUS_SESSION_BUS_ADDRESS @(cat @dbus_add_file)))
   ((file ~bin-dir init-checkconf) -d @tmpfile)
   (set! rv @?)
   (rm @tmpfile :force true)
   (exit @rv)))

(defn configure
  "Write out job definitions."
  [{:keys [instance-id]}]
  (let [{:keys [bin-dir jobs service-dir verify-dir]}
        (get-settings :upstart {:instance-id instance-id})]
    (doseq [[job options] jobs
            :let [job-name (name job)
                  path (fragment (file ~service-dir ~(str job-name ".conf")))
                  verify-path (fragment (file ~verify-dir "verify_upstart"))]]
      (directory verify-dir)
      (remote-file
       verify-path
       :content (verify-conf-script bin-dir)
       :mode "755"
       :literal true)
      (remote-file
       path
       :content (job-format options)
       :literal true
       :verify (fragment
                ((sudo :user ~(:username (admin-user))) ~verify-path))))))

;;; ## Service Control

(defmethod service-supervisor :upstart
  [_
   {:keys [service-name]}
   {:keys [action if-flag if-stopped instance-id]
    :or {action :start}
    :as options}]
  (let [{:keys [sbin-dir service-dir]} (get-settings :upstart options)
        prog (fn [x] (fragment (file ~sbin-dir ~x)))
        override-path (fragment
                       (file ~service-dir
                             ~(str (name service-name) ".override")))]
    (case action
      :enable (exec-checked-script
               (str "Enable upstart service " service-name)
               (rm ~override-path :force true))
      :disable (remote-file ~override-path :contents "manual")
      :start-stop (debugf
                   "Trying to start-stop job %s, but upstart doesn't support"
                   service-name)
      (if if-flag
        (plan-when (target-flag? if-flag)
          (exec-checked-script
           (str (name action) " " service-name)
           ((prog ~action) (quoted ~(name service-name)))))
        (if if-stopped
          (exec-checked-script
           (str (name action) " " service-name)
           (if-not ((pipe ((prog "status") ~(name service-name))
                          ("grep" "running")))
             ((prog ~action) (quoted ~(name service-name)))))
          (if (= action :start)
            ;; upstart reports an error if we try starting when already
            ;; running
            (exec-checked-script
             (str (name action) " " service-name)
             (if-not ((pipe ((prog "status") ~(name service-name))
                            ("grep" "running")))
               ((prog ~action) (quoted ~(name service-name)))))
            (exec-checked-script
             (str (name action) " " service-name)
             ((prog ~action) (quoted ~(name service-name))))))))))


;;; ## Server Spec
(defn server-spec [settings & {:keys [instance-id] :as options}]
  (api/server-spec
   :phases {:settings (plan-fn
                        (pallet.crate.upstart/settings
                         (merge settings options)))
            :install (plan-fn (install options))
            :configure (plan-fn (configure options))}))
