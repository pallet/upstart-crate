(ns pallet.crate.upstart
  "Create upstart daemon scripts"
  (:require
   [pallet.parameter :as parameter]
   [pallet.action.package :as package]
   [pallet.action.remote-file :as remote-file]
   [clojure.string :as string]))

(defn package
  "Install upstart from system package"
  [session]
  (->
   session
   (package/packages
    :yum ["upstart"]
    :aptitude ["upstart"])))

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
         :description :author :version :emits
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
  [session name &
   {:keys [script exec
           pre-start-script post-start-script pre-stop-script post-stop-script
           pre-start-exec post-start-exec pre-stop-exec post-stop-exec
           start-on stop-on
           env export respawn respawn-limit normal-exit
           instance
           description author version emits
           console umask nice oom chroot chdir limit
           kill-timeout expect] :as options}]
  (->
   session
   (remote-file/remote-file
    (format "/etc/init/%s.conf" name)
    :content (job-format options)
    :literal true)
   (parameter/assoc-for-target [:upstart (keyword name)] options)))
