;;; Pallet project configuration file

(require
 '[pallet.crate.upstart-test
   :refer [upstart-test-spec]]
 '[pallet.crates.test-nodes :refer [node-specs]])

(defproject upstart-crate
  :provider node-specs                  ; supported pallet nodes
  :groups [(group-spec "upstart-test"
             :extends [with-automated-admin-user
                       upstart-test-spec]
             :roles #{:live-test :default :upstart})])
