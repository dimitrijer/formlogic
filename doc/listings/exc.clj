;; handlers.clj
(defn wrap-catch-exceptions
  "Middleware that catches all exceptions in business logic
  and returns a status 500 response with stack trace attached."
  [handler]
  (fn [req]
    (try (handler req)
         (catch Throwable e
           (do
             (log/error e "Unhandled exception in handler!")
             ;; We wrap exception and stack trace in response
             ;; body. This will get picked up by wrap-500.
             (resp/internal-server-error
               (str/join "\n" (conj (map str (.getStackTrace e))
                                    (.toString e)))))))))

(defn wrap-500
  "Middleware that replaces all 500 responses with a custom page."
  [handler]
  (fn [req]
    (let [response-map (handler req)
          status (:status response-map)]
      (if (= 500 status)
        (-> (resp/internal-server-error
              (views/internal-error-page (:body response-map)))
            (resp/content-type "text/html")
            (resp/charset "utf-8"))
        response-map))))

;; views.clj
(defn internal-error-page 
  "Renders a nice 500 page with error details."
  ([] (internal-error-page nil))
  ([details]
   (page-template
     "500 - Interna @greška@"
     (wide-well
       [:h1 {:class "text-danger"} "Interna @greška@!"]
       [:p "@Nešto@ nije u redu, radimo na tome..."]
       (button-link "/" "@Početna@ stranica"))
     (when details
       [:div {:class "im-centered-wide"}
        [:p "Detalji:"]
        [:pre (h details)]]))))

;; routes.clj
;; Main app route with all middleware wrapped.
(def app (-> #'site-routes
             wrap-json-response
             wrap-json-params
             (wrap-defaults site-defaults)
             handlers/wrap-catch-exceptions
             handlers/wrap-log-request
             handlers/wrap-500))
