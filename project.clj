(defproject com.palletops/upstart-crate "0.8.0-SNAPSHOT"
  :description "Crate for Upstart installation"
  :url "http://github.com/pallet/riemann-crate"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [com.palletops/pallet "0.8.0-beta.6"]]
  :resource {:resource-paths ["doc-src"]
             :target-path "target/classes/pallet_crate/upstart_crate/"
             :includes [#"doc-src/USAGE.*"]}
  :prep-tasks ["resource" "crate-doc"])
