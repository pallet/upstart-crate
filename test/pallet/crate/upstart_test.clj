(ns pallet.crate.upstart-test
  (:use
   clojure.test
   pallet.test-utils)
  (:require
   [pallet.actions :refer [remote-file]]
   [pallet.api :refer [plan-fn] :as api]
   [pallet.build-actions :as build-actions]
   [pallet.crate.service :refer [service-supervisor-config]]
   [pallet.crate.service-test :refer [service-supervisor-test]]
   [pallet.crate.upstart :as upstart]
   [pallet.stevedore :refer [fragment]]))

(deftest invoke-test
  (is (build-actions/build-actions {}
        (upstart/settings {})
        (upstart/install)
        (upstart/job "abc"
                     :script "ls"
                     :description "Hussein B."
                     :pre-start-exec "ifup -a"
                     :limit {"disk" "100 200"}
                     :env "HOME=/home"
                     :export ["HOME" "AWAY"]
                     :respawn true)
        (upstart/jobs))))

(defn upstart-test [config]
  (service-supervisor-test :upstart config {}))

(def upstart-test-spec
  (let [config {:service-name "myjob" :exec "/tmp/myjob"}]
    (api/server-spec
     :extends [(upstart/server-spec {})]
     :phases {:settings (plan-fn (service-supervisor-config :upstart config {}))
              :configure (plan-fn
                           (remote-file
                            "/tmp/myjob"
                            :content (fragment ("sleep" 100000000))
                            :mode "0755"))
              :test (plan-fn (upstart-test config))})))
