(ns formlogic.routes
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.util.response :as resp]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.json :refer [wrap-json-response, wrap-json-params]]
            [formlogic.handlers :as handlers]
            [formlogic.views :as views]
            [formlogic.controllers :as controllers]))

(defroutes unauthenticated-routes
  (GET "/" [] (resp/redirect "/login"))
  (GET "/login" [] (views/login-page))
  (POST "/login" [email password] (controllers/login email password))
  (GET "/register" [] (views/register-page))
  (POST "/register" [email] (controllers/register email)))

(defroutes user-routes
  (context "/user/:user-id" [user-id]
    (GET "/" []  (str "<p>Olala " user-id "</p>"))))

(defroutes default-routes
  (route/not-found views/not-found-page))

(defn wrap-basic-middleware [some-routes]
  (-> some-routes
      wrap-json-response
      wrap-json-params
      (wrap-defaults site-defaults)
      handlers/wrap-catch-exceptions
      handlers/wrap-log-request
      handlers/wrap-500))

;; Note that every time we reload this page, a new in-memory session store is
;; created, thus invalidating all previous session IDs.
(def app (routes
           (wrap-basic-middleware #'unauthenticated-routes)
           (wrap-basic-middleware #'user-routes)
           (wrap-basic-middleware #'default-routes)))
