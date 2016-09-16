(ns formlogic.views
  (:require [hiccup.element :as elem]
            [formlogic.user.account :as account])
  (:use [hiccup.page :only (html5 include-css include-js)]
        [hiccup.core :only (h)]
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
     [:h1 {:class "text-warning"} "Stranica nije nađena!"]
     [:p "Tražena stranica ne postoji."]
     (elem/link-to {:class "btn btn-primary"} "/" "Početna stranica")]))

(defn internal-error-page 
  ([]
   (internal-error-page nil))
  ([details]
   (page-template
     "500 - Interna greška"
     [:div {:class "im-centered-wide well"}
      [:h1 {:class "text-danger"} "Interna greška!"]
      [:p "Nešto nije u redu, radimo na tome..."]
      (elem/link-to {:class "btn btn-primary"} "/" "Početna stranica")]
     (when details
       [:div {:class "im-centered-wide"}
        [:p "Detalji:"]
        [:pre (h details)]]))))

;; Should be a function, since *anti-forgery-token* is bound to a session, and
;; this is defined as soon as source is eval'd.
(defn login-page []
  (page-template
    "Login"
    [:div {:class "im-centered well"}
     [:h1 {:class "text-center text-info"} "Login"]
     [:hr]
     [:form {:name "loginForm"
             :novalidate ""
             :role "form"
             :method "post"
             :action "/login"}
      (anti-forgery-field)
      [:div {:class "form-group row"}
       (label {:class "col-lg-2 col-form-label control-label"} "email" "Email")
       [:div {:class "col-lg-10"}
        (email-field {:class "form-control"
                      :required ""
                      :placeholder "Vaš mail"
                      :ng-pattern "/.*@(.*\\.)?etf\\.rs$/"
                      :name "userEmail"
                      :ng-model "user.email"} "email")]]
      [:div {:class "row alert alert-danger" :role "alert" :ng-show "loginForm.userEmail.$error.pattern"}
       [:span {:class "col-lg-1 glyphicon glyphicon-exclamation-sign" :aria-hidden "true"}]
       [:span {:class "col-lg-1 sr-only"} "Error: "]
       [:span {:class "col-lg-11"} "Unesite ispravnu email adresu (npr. rd090112d@student.etf.rs)"]]
      [:div {:class "row form-group"}
       (label {:class "col-lg-2 col-form-label control-label"} "password" "Lozinka")
       [:div {:class "col-lg-10"}
        (password-field {:class "form-control"
                         :required ""
                         :placeholder "Vaša lozinka"
                         :ng-model "user.password"} "password")]]
      [:hr]
      [:div {:class "row"}
       [:div {:class "col-lg-6"}
        [:button {:class "btn btn-primary btn-block" :type "submit" :ng-disabled "!(loginForm.$valid)"} "Prijava"]]
       [:div {:class "col-lg-4"}
        (elem/link-to {:class "btn btn-default btn-block"} "/register" "Registracija")]]]]))

(defn register-page []
  (page-template
    "Registracija"
    [:div {:class "im-centered well"}
     [:h1 {:class "text-center text-info"} "Registracija"]
     [:hr]
     [:form {:name "registerForm"
             :novalidate ""
             :role "form"
             :method "post"
             :action "/register"}
      (anti-forgery-field)
      [:div {:class "form-group row"}
       (label {:class "col-lg-2 col-form-label control-label"} "email" "Email")
       [:div {:class "col-lg-10"}
        (email-field {:class "form-control"
                      :required ""
                      :placeholder "Vaš mail"
                      :ng-pattern "/.*@(.*\\.)?etf\\.rs$/"
                      :name "email"
                      :ng-model "user.email"} "email")]]
      [:div {:class "row alert alert-danger" :role "alert" :ng-show "registerForm.email.$error.pattern"}
       [:span {:class "col-lg-1 glyphicon glyphicon-exclamation-sign" :aria-hidden "true"}]
       [:span {:class "col-lg-1 sr-only"} "Error: "]
       [:span {:class "col-lg-11"} "Unesite ispravnu email adresu (npr. rd090112d@student.etf.rs)"]]
      [:hr]
      [:div {:class "row"}
       [:div {:class "col-lg-offset-3 col-lg-6"}
        [:button {:class "btn btn-primary btn-block" :type "submit" :ng-disabled "!(registerForm.$valid)"} "Izvrši"]]]]]))

(defn register-success [email]
  (page-template "Registracija uspešna!"
     [:div {:class "im-centered-wide well"}
      [:h1 {:class "text-success"} "Registracija uspešna"]
      [:p "Na Vašu email adresu " [:strong (h email)] " će kroz nekoliko minuta stići lozinka."]
      (elem/link-to {:class "btn btn-primary"} "/" "Početna stranica")]))
