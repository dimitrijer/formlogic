(defproject formlogic "0.1.0-SNAPSHOT"
  :description "formlogic app"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 ; Logging dependencies.
                 [org.clojure/tools.logging "0.3.1"]
                 [org.slf4j/slf4j-api "1.7.21"]
                 [org.apache.logging.log4j/log4j-slf4j-impl "2.6.2"]
                 [org.apache.logging.log4j/log4j-api "2.6.2"]
                 [org.apache.logging.log4j/log4j-core "2.6.2"]
                 ; Configuration library.
                 [levand/immuconf "0.1.0"]
                 ; Ring + Compojure
                 [compojure "1.5.1"]
                 [ring/ring-jetty-adapter "1.5.0"]
                 ; ring-defaults simplifies adding middleware for sane defaults.
                 [ring/ring-defaults "0.2.1"]]
  :main ^:skip-aot formlogic.core
  :target-path "target/%s"
  :plugins [[lein-ring "0.9.7"]]
  :ring {:handler formlogic.handler/app
         :nrepl {:start? true
                 :port 9998}}
  :profiles {:uberjar {:aot :all}
             :dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                                  [ring/ring-mock "0.3.0"]]}})
