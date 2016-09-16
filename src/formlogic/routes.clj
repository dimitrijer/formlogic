(ns formlogic.routes
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.util.response :as resp]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [formlogic.handlers :as handlers]
            [formlogic.views :as views]))

(defroutes unauthenticated-routes
  (GET "/" [] (resp/redirect "/login"))
  (GET "/login" [] (views/login-page))
  (GET "/register" [] (views/register-page))
  (POST "/login" [& params] (handlers/login params)))

(defroutes user-routes
  (context "/user/:user-id" [user-id]
    (GET "/" []  (str "<p>Olala " user-id "</p>"))))

(defroutes default-routes
  (route/not-found views/not-found-page))

(defn wrap-basic-middleware [some-routes]
  (-> (wrap-defaults some-routes site-defaults)
      handlers/wrap-catch-exceptions
      handlers/wrap-log-request
      handlers/wrap-500))

(def app (routes
           (wrap-basic-middleware #'unauthenticated-routes)
           (wrap-basic-middleware #'user-routes)
           (wrap-basic-middleware #'default-routes)))
