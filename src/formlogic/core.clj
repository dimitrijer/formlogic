(ns formlogic.core
  (:require [clojure.tools.logging :as log]
            [immuconf.config :as conf]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.adapter.jetty :refer [run-jetty]]
            [formlogic.routes :refer [app-routes]])
  (:gen-class))

(def cfg (conf/load "resources/config.edn"))

(def cget (partial conf/get cfg))

(def app
  (wrap-defaults app-routes site-defaults))

(defn -main
  "Entry point of the app."
  [& args]
  (run-jetty app {:port 3000 :join? false}))
