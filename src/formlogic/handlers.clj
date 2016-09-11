(ns formlogic.handlers
  (:require [clojure.tools.logging :as log]
            [ring.util.http-status :as status]
            [clojure.string :as str]))

(defn wrap-log-request [handler]
  "Middleware that logs each request."
  (fn [req]
    (let [{:keys [uri remote-addr]} req
          current-time (System/currentTimeMillis)
          request-method (str/upper-case (:request-method req))]
      (log/debugf "Started %s [%s] by (%s)" request-method uri remote-addr)
      ; Let evaluates to last form, so, for sake of next middleware, we need to make sure
      ; it evaluates to result of calling handler function.
      (let [response-map (handler req)
            status-code (:status response-map)
            status-name (status/get-name status-code)
            elapsed-time (- (System/currentTimeMillis) current-time)]
        (log/debugf "Finished %s [%s => %d %s] in %dms by (%s)"
                    request-method
                    uri
                    status-code
                    status-name
                    elapsed-time
                    remote-addr)
        response-map))))

(defn wrap-catch-exceptions [handler]
  "Middleware that catches all exceptions in business logic."
  (fn [req]
    (try (handler req)
         (catch Exception e
           (do
             (log/error e "Unhandled exception in handler!")
             {:status 500})))))

(defn login [params]
  (let [{:keys [email password]} params]
    (throw (NullPointerException.))
    (log/spy params)
    {:status 400}))
