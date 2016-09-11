(ns formlogic.handlers
  (:require [clojure.tools.logging :as log]
            [clojure.string :as str]
            [ring.util.http-status :as status]
            [ring.util.http-response :as resp]
            [formlogic.views :as views]))

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
             ;; We wrap exception and stack trace in response body. These will
             ;; get picked up by wrap-500 and it will make a nice HTML page.
             (resp/internal-server-error
               (str/join "\n" (conj (map str (.getStackTrace e)) (.toString e)))))))))

(defn wrap-500 [handler]
  "Middleware that replaces all 500 responses with a custom page."
  (fn [req]
    (let [response-map (handler req)
          status (:status response-map)]
      (if (= 500 status)
        (-> (resp/internal-server-error (views/internal-error-page (:body response-map)))
            (resp/content-type "text/html")
            (resp/charset "utf-8"))
        response-map))))

(defn login [params]
  (let [{:keys [email password]} params]
    (resp/internal-server-error "amigaaaawd")))
