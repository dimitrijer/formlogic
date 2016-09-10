(ns formlogic.core
  (:require [clojure.tools.logging :as log]
            [immuconf.config :as conf]
            [ring.adapter.jetty :refer [run-jetty]]
            [formlogic.routes :refer [app]])
  (:gen-class))

(def cfg (conf/load "resources/config.edn"))

(def cget (partial conf/get cfg))

(defn -main
  "Entry point of the app."
  [& args]
  (let [port (cget :server :port)]
    (run-jetty app {:port port :join? false})
    (log/infof "Started server on port %d." port)))
