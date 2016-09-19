(ns formlogic.handlers
  (:require [clojure.tools.logging :as log]
            [clojure.string :as str]
            [ring.util.http-status :as status]
            [ring.util.http-response :as resp]
            [formlogic.views :as views]))

(defn wrap-log-request
  "Middleware that logs each request."
  [handler]
  (fn [req]
    (let [{:keys [uri remote-addr]} req
          current-time (System/currentTimeMillis)
          request-method (str/upper-case (:request-method req))]
      (log/debugf "Started %s [%s] by (%s)"
                  request-method
                  uri
                  remote-addr)
      ;; Let evaluates to last form, so, for sake of next middleware, we need to make sure
      ;; it evaluates to result of calling handler function. Also, check if response
      ;; map is nil, since this route may have not been matched.
      (if-let [response-map (handler req)]
        (let [status-code (:status response-map)
              status-name (status/get-name status-code)
              elapsed-time (- (System/currentTimeMillis) current-time)]
          (log/debugf "Finished %s [%s => %d %s] in %dms by (%s)"
                      request-method
                      uri
                      status-code
                      status-name
                      elapsed-time
                      remote-addr)
          response-map)))))

(defn wrap-catch-exceptions
  "Middleware that catches all exceptions in business logic."
  [handler]
  (fn [req]
    (try (handler req)
         (catch Throwable e
           (do
             (log/error e "Unhandled exception in handler!")
             ;; We wrap exception and stack trace in response body. These will
             ;; get picked up by wrap-500 and it will make a nice HTML page.
             (resp/internal-server-error
               (str/join "\n" (conj (map str (.getStackTrace e)) (.toString e)))))))))

(defn wrap-500
  "Middleware that replaces all 500 responses with a custom page."
  [handler]
  (fn [req]
    (let [response-map (handler req)
          status (:status response-map)]
      (if (= 500 status)
        (-> (resp/internal-server-error (views/internal-error-page (:body response-map)))
            (resp/content-type "text/html")
            (resp/charset "utf-8"))
        response-map))))

(defn wrap-user-session-check
  "Middleware which asserts that user handler is being invoked for the right
  user by inspecting session."
  [handler]
  (fn [{session :session :as req}]
    (let [response-map (handler req)]
      (if-let [user (:user session)]
        response-map
        (do
          (log/error "Tried to invoke user handler without appropriate session!")
          (-> (resp/forbidden views/forbidden-page)
              (resp/content-type "text/html")
              (resp/charset "utf-8")
              ;; Clear session.
              (assoc :session nil)))))))
