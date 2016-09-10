(ns formlogic.routes
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [formlogic.handlers :refer [wrap-log-request]]
            [formlogic.views :as views]))

(defroutes app-routes
  (GET "/" [] "Hello World")
  (GET "/login" [] (views/page "Login" [:h1 "Olalala"]))
  (route/not-found "Not Found"))

(def app
  (-> (wrap-defaults app-routes site-defaults)
      wrap-log-request))
