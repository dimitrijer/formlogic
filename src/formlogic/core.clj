(ns formlogic.core
  (:require [clojure.tools.logging :as log]
            [immuconf.config :as conf]
            [ring.adapter.jetty :refer [run-jetty]]
            [formlogic.routes :refer [app]])
  (:gen-class))

(def cfg (conf/load "resources/config.edn"))

(def cget (partial conf/get cfg))

;; By using defonce we prevent multiple evaluation during REPL reload. It also
;; allows us to start and stop the server interactively. Passing app as var
;; makes it possible to redefine routes/handlers while the server is running.
(defonce server (run-jetty #'app {:port (cget :server :port) :join? false}))

(defn -main
  "Entry point of the app."
  [& args]
  (let [port (cget :server :port)]
    server
    (log/infof "Started server on port %d." port)))
