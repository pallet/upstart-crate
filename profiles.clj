{:dev
 {:dependencies [[com.palletops/pallet "0.8.0-beta.6" :classifier "tests"]
                 [com.palletops/crates "0.1.0"]
                 [ch.qos.logback/logback-classic "1.0.9"]]
  :plugins [[com.palletops/pallet-lein "0.6.0-beta.9"]
            [lein-set-version "0.3.0"]
            [lein-resource "0.3.2"]
            [codox/codox.leiningen "0.6.4"]
            [lein-marginalia "0.7.1"]]
  :aliases {"live-test-up"
            ["pallet" "up"
             "--phases" "install,configure,test"
             "--selectors" "live-test"]
            "live-test-down" ["pallet" "down" "--selector" "live-test"]
            "live-test" ["do" "live-test-up," "live-test-down"]}}
 :doc {:dependencies [[com.palletops/pallet-codox "0.1.0-SNAPSHOT"]]
       :plugins [[codox/codox.leiningen "0.6.4"]
                 [lein-marginalia "0.7.1"]]
       :codox {:writer codox-md.writer/write-docs
               :output-dir "doc/0.8/api"
               :src-dir-uri "https://github.com/pallet/upstart-crate/blob/develop"
               :src-linenum-anchor-prefix "L"}
       :aliases {"marg" ["marg" "-d" "doc/0.8/annotated"]
                 "codox" ["doc"]
                 "doc" ["do" "codox," "marg"]}}
 :release
 {:set-version
  {:updates [{:path "README.md" :no-snapshot true}]}}
 :no-checkouts {:checkout-deps-shares ^:replace []} ; disable checkouts
 :jclouds {:dependencies
           [[com.palletops/pallet-jclouds "1.5.3"]
            [org.jclouds/jclouds-allblobstore "1.5.5"]
            [org.jclouds/jclouds-allcompute "1.5.5"]
            [org.jclouds.driver/jclouds-slf4j "1.5.5"
             :exclusions [org.slf4j/slf4j-api]]
            [org.jclouds.driver/jclouds-sshj "1.5.5"]]}}
