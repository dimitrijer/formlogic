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
           (GET "/" [] (if (:admin user)
                         (views/administrator-page user)
                         (views/assignments-page user)))
           (GET "/logout" [] (controllers/logout session))
           (GET "/progress/:assignment-id" [assignment-id]
                (controllers/attach-progress session user (Integer/parseInt assignment-id)))
           (GET "/progress/:assignment-id/:task" [assignment-id task]
                (or (controllers/render-task session
                                             (Integer/parseInt assignment-id)
                                             (Integer/parseInt task))
                    (-> (resp/forbidden views/forbidden-page)
                        (resp/content-type "text/html")
                        (resp/charset "utf-8"))))
           (POST "/progress/:assignment-id/:task" [assignment-id task continue]
                 (or (controllers/save-task session
                                            (Integer/parseInt assignment-id)
                                            (Integer/parseInt task)
                                            (:form-params req)
                                            (Boolean/parseBoolean continue))
                     (-> (resp/forbidden views/forbidden-page)
                         (resp/content-type "text/html")
                         (resp/charset "utf-8"))))
           ;; The following are supposed to be invoked by admins only.
           (GET "/students" [email]
                (controllers/search-students user email))
           (GET "/assignments" [assignment_name]
                (controllers/search-assignments user assignment_name))
           (GET "/students/progresses" [id]
                (controllers/get-progresses-for-student user id))
           (GET "/assignments/progresses" [id]
                (controllers/get-progresses-for-assignment user id))
           (GET "/progresses/:progress-id" [progress-id]
                (controllers/attach-progress session (Integer/parseInt progress-id)))))

(defroutes site-routes
  (GET "/" {session :session} (if (contains? session :user)
                                (resp/found "/user/")
                                (resp/found "/login")))
  (GET "/login" [] (views/login-page))
  (POST "/login" [email password :as req] (controllers/login email password req))
  (GET "/register" [] (views/register-page))
  (POST "/register" [email] (controllers/register email))
  (wrap-routes #'user-routes handlers/wrap-user-session-check)
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
