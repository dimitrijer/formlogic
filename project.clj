(defproject formlogic "0.1.0-SNAPSHOT"
  :description "formlogic app"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 ; Logging dependencies.
                 [org.clojure/tools.logging "0.3.1"]
                 [org.slf4j/slf4j-api "1.7.21"]
                 [org.apache.logging.log4j/log4j-slf4j-impl "2.6.2"]
                 [org.apache.logging.log4j/log4j-api "2.6.2"]
                 [org.apache.logging.log4j/log4j-core "2.6.2"]]
  :main ^:skip-aot formlogic.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
