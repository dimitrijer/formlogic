(ns formlogic.routes
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.util.http-response :as resp]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.json :refer [wrap-json-response, wrap-json-params]]
            [formlogic.handlers :as handlers]
            [formlogic.views :as views]
            [clojure.tools.logging :as log]
            [formlogic.controllers :as controllers]))

(defroutes user-routes
  (context "/user" {{user :user :as session} :session :as req}
    (GET "/" [] (views/user-page user))
    (GET "/progress/:assignment-id" [assignment-id] (controllers/attach-progress
                                                      session
                                                      user
                                                      assignment-id))
    (GET "/progress/:assignment-id/:task" [assignment-id task]
         (or (controllers/render-task session assignment-id task)
             (-> (resp/forbidden views/forbidden-page)
                 (resp/content-type "text/html")
                 (resp/charset "utf-8"))))
    (POST "/answers" [question1] (log/spy question1))))

(defroutes site-routes
  (GET "/" {session :session} (if (contains? session :user)
                                (resp/found "/user/")
                                (resp/found "/login")))
  (GET "/login" [] (views/login-page))
  (POST "/login" [email password :as req] (controllers/login email password req))
  (GET "/register" [] (views/register-page))
  (POST "/register" [email] (controllers/register email))
  (handlers/wrap-user-session-check #'user-routes)
  (route/not-found views/not-found-page))

;; Note that every time we reload this page, a new in-memory session store is
;; created, thus invalidating all previous session IDs.
(def app (-> #'site-routes
             wrap-json-response
             wrap-json-params
             (wrap-defaults site-defaults)
             handlers/wrap-catch-exceptions
             handlers/wrap-log-request
             handlers/wrap-500))
