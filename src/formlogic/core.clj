(ns formlogic.core
  (:require [clojure.tools.logging :as log]
            [immuconf.config :as conf]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.adapter.jetty :refer [run-jetty]]
            [formlogic.routes :refer [app-routes]]
            [clojure.string :as str])
  (:gen-class))

(def cfg (conf/load "resources/config.edn"))

(def cget (partial conf/get cfg))

(defn wrap-log-request [handler]
  "Middleware that logs each request."
  (fn [req]
    (let [{:keys [request-method uri]} req
          current-time (System/currentTimeMillis)]
      (log/debugf "Started %s [%s]." (str/upper-case request-method) uri)
      (handler req)
      (log/debugf "Finished %s [%s] in %dms." (str/upper-case request-method) uri (- (System/currentTimeMillis) current-time)))))

(def app
  (wrap-defaults (wrap-log-request app-routes) site-defaults))

(defn -main
  "Entry point of the app."
  [& args]
  (let [port (cget :server :port)]
    (run-jetty app {:port port :join? false})
    (log/infof "Started server on port %d." port)))
