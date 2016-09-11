(ns formlogic.routes
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.util.response :as resp]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [formlogic.handlers :as handlers]
            [formlogic.views :as views]))

(defroutes app-routes
  (GET "/" [] () (resp/redirect "/login"))
  (GET "/login" [] (views/login-page))
  (GET "/register" [] (views/register-page))
  (POST "/login" [& params] (handlers/login params))
  (route/not-found views/not-found-page))

(def app
  (-> (wrap-defaults #'app-routes site-defaults)
      handlers/wrap-catch-exceptions
      handlers/wrap-log-request
      handlers/wrap-500))
