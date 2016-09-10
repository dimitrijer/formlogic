(ns formlogic.core
  (:require [clojure.tools.logging :as log]
            [immuconf.config :as conf])
  (:gen-class))

(def cfg (conf/load "resources/config.edn"))
(def cget (partial conf/get cfg))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (log/spy (cget :prop :kw))
  (log/spy (conf/get cfg :prop :kw))
  (log/error "Test error")
  (println "Hello, World!"))
