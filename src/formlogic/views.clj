(ns formlogic.views
  (:require [hiccup.element :as elem])
  (:use [hiccup.page :only (html5 include-css include-js)]
        [hiccup.form]
        [ring.util.anti-forgery]))

(defn page-template
  "Returns HTML page with provided title and contents."
  [title & contents]
  (html5 {:lang "rs" :ng-app "myApp"}
         [:head
          [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
          [:title title]
          (include-css "css/style.css")
          (include-css "css/bootstrap.min.css")
          (include-js "js/angular.min.js")
          (include-js "js/ui-bootstrap-tpls-2.1.3.min.js")
          (include-js "js/script.js")]
         [:body [:div {:class "container"} contents]]))

(def not-found-page
  (page-template
    "404 - Stranica ne postoji"
    [:div {:class "im-centered well"}
     [:h1 {:class "info-warning"} "Stranica nije nađena!"]
     [:p "Tražena stranica ne postoji."]
     (elem/link-to {:class "btn btn-primary"} "/" "Početna stranica")]))

;; Should be a function, since *anti-forgery-token* is bound to a session, and
;; this is defined as soon as source is eval'd
(defn login-page []
  (page-template
    "Login"
    [:div {:class "im-centered well"}
     [:h1 {:class "text-center text-info"} "Login"]
     [:hr]
     [:form {:novalidate "" :role "form" :method "post" :action "/login"}
      (anti-forgery-field)
      [:div {:class "form-group"}
       (label {:class "control-label"} "email" "Email")
       (email-field {:class "form-control"
                     :placeholder "Vaš mail (npr. rd090112d@student.etf.rs)"
                     :ng-model "user.email"} "user.email")]
      [:div {:class "form-group"}
       (label {:class "control-label"} "password" "Lozinka")
       (password-field {:class "form-control"
                        :placeholder "Vaša lozinka"
                        :ng-model "user.password"} "user.password")]
      [:hr]
      [:div {:class "row"}
       [:div {:class "col-lg-6"}
        [:button {:class "btn btn-primary btn-block" :type "submit"} "Prijava"]]
       [:div {:class "col-lg-4"}
        (elem/link-to {:class "btn btn-default bto-block"} "/register" "Registracija")]]]]))
