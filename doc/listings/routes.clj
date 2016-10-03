;; routes.clj
(defroutes site-routes
  (GET "/" {session :session} (if (contains? session :user)
                                (resp/found "/user/")
                                (resp/found "/login")))
  (GET "/login" [] (views/login-page))
  (POST "/login" [email password :as r]
        (controllers/login email password r))
  (GET "/register" [] (views/register-page))
  (POST "/register" [email] (controllers/register email))
  ;; wrap-routes will invoke wrapped handlers only if route matches.
  (wrap-routes #'user-routes handlers/wrap-user-session-check)
  ;; Custom 404 page.
  (route/not-found views/not-found-page))

;; views.clj
(def not-found-page
  (page-template
    "404 - Stranica ne postoji"
    (well
      [:h1 {:class "text-warning"} "Stranica nije @nađena@!"]
      [:p "@Tražena@ stranica ne postoji."]
      (button-link "/" "@Početna@ stranica"))))
