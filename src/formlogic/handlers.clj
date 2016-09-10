(ns formlogic.handlers
  (:require [clojure.tools.logging :as log]
            [clojure.string :as str]))

(defn wrap-log-request [handler]
  "Middleware that logs each request."
  (fn [req]
    (let [{:keys [request-method uri remote-addr]} req
          current-time (System/currentTimeMillis)]
      (log/debugf "Started %s [%s] by (%s)" (str/upper-case request-method) uri remote-addr)
      ; Let evaluates to last form, so, for sake of next middleware, we need to make sure
      ; it evaluates to result of calling handler function.
      (let [result (handler req)]
        (log/debugf "Finished in %dms by (%s)" (- (System/currentTimeMillis) current-time) remote-addr)
        result))))

