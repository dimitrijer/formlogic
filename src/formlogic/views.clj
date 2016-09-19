(ns formlogic.views
  (:require [hiccup.element :as elem]
            [formlogic.user.account :as account]
            [clojure.string :as str])
  (:use [hiccup.page :only (html5 include-css include-js)]
        [hiccup.core :only (h html)]
        [hiccup.form]
        [ring.util.anti-forgery]))

(defn page-template
  "Returns HTML page with provided title and contents."
  [title & contents]
  (html5 {:lang "rs" :ng-app "myApp"}
         [:head
          [:meta {:charset "utf-8"}]
          [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge"}]
          [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
          [:title (h title)]
          (include-js "/js/angular.min.js"
                      "/js/angular-sanitize.min.js")
          (include-js "/js/ui-bootstrap-tpls-2.1.3.min.js")
          (include-js "/js/script.js")
          (include-css "/css/style.css")
          (include-css "/css/bootstrap.min.css")
          [:body [:div {:class "container"} contents]]]))

(defn button-link
  ([uri btn-label] (button-link uri btn-label "btn btn-primary"))
  ([uri btn-label btn-class]
   (elem/link-to {:class btn-class} uri btn-label)))

(defn well
  [& contents]
  [:div {:class "im-centered well"} contents])

(defn wide-well
  [& contents]
  [:div {:class "im-centered-wide well"} contents])

(defn ng-alert
  [alert-class div-args span-args & contents]
  [:div (into {:class (str "alert " alert-class) :role "alert"} div-args)
   [:span {:class "col-lg-1 glyphicon glyphicon-alert" :aria-hidden "true"}]
   [:span (into {:class "col-lg-11"} span-args) contents]])

(def not-found-page
  (page-template
    "404 - Stranica ne postoji"
    (well
      [:h1 {:class "text-warning"} "Stranica nije nađena!"]
      [:p "Tražena stranica ne postoji."]
      (button-link "/" "Početna stranica"))))

(def forbidden-page
  (page-template
    "403 - Zabranjen pristup"
    (well
      [:h1 {:class "text-danger"} "Pristup onemogućen!"]
      [:p "Pristup resursu je onemogućen usled nedovoljnih privilegija."]
      (button-link "/" "Početna stranica"))))

(defn internal-error-page 
  ([]
   (internal-error-page nil))
  ([details]
   (page-template
     "500 - Interna greška"
     (wide-well
       [:h1 {:class "text-danger"} "Interna greška!"]
       [:p "Nešto nije u redu, radimo na tome..."]
       (button-link "/" "Početna stranica"))
     (when details
       [:div {:class "im-centered-wide"}
        [:p "Detalji:"]
        [:pre (h details)]]))))

;; Should be a function, since *anti-forgery-token* is bound to a session, and
;; this is defined as soon as source is eval'd.
(defn login-page
  []
  (page-template
    "Login"
    (well
      (include-js "/js/md5.min.js" "/js/login.js")
      [:h1 {:class "text-center text-info"} "Login"]
      [:hr]
      [:form {:name "loginForm"
              :novalidate ""
              :role "form"
              :ng-controller "LoginFormController"
              :ng-submit "login()"}
       (anti-forgery-field)
       (ng-alert "row alert-danger" {:ng-show "alert != null"} {:ng-bind-html "alert"})
       [:div {:class "form-group row"}
        (label {:class "col-lg-2 col-form-label control-label"} "email" "Email")
        [:div {:class "col-lg-10"}
         (email-field {:class "form-control"
                       :required ""
                       :placeholder "Vaš mail"
                       :ng-pattern "/.*@(.*\\.)?etf\\.rs$/"
                       :ng-model "user.email"} "email")]]
       (ng-alert "row alert-warning"
                 {:ng-show "loginForm.email.$error.pattern"}
                 {}
                 "Unesite ispravnu email adresu (npr. rd090112d@student.etf.rs)")
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
         (button-link "/register" "Registracija" "btn btn-default btn-block")]]])))

(defn register-page
  [& {:keys [alert]}]
  (page-template
    "Registracija"
    (well
      [:h1 {:class "text-center text-info"} "Registracija"]
      [:hr]
      [:form {:name "registerForm"
              :novalidate ""
              :role "form"
              :method "post"
              :action "/register"}
       (anti-forgery-field)
       (when alert (ng-alert "row alert-danger" {} {} alert))
       [:div {:class "form-group row"}
        (label {:class "col-lg-2 col-form-label control-label"} "email" "Email")
        [:div {:class "col-lg-10"}
         (email-field {:class "form-control"
                       :required ""
                       :placeholder "Vaš mail"
                       :ng-pattern "/.*@(.*\\.)?etf\\.rs$/"
                       :name "email"
                       :ng-model "user.email"} "email")]]
       (ng-alert "row alert-warning"
                 {:ng-show "registerForm.email.$error.pattern"}
                 {}
                 "Unesite ispravnu email adresu (npr. rd090112d@student.etf.rs)")
       [:hr]
       [:div {:class "row"}
        [:div {:class "col-lg-offset-3 col-lg-6"}
         [:button {:class "btn btn-primary btn-block" :type "submit" :ng-disabled "!(registerForm.$valid)"} "Izvrši"]]]])))

(defn register-success [email]
  (page-template
    "Registracija uspešna!"
    (wide-well
      [:h1 {:class "text-success"} "Registracija uspešna"]
      [:p "Na Vašu email adresu " [:strong (h email)] " će kroz nekoliko minuta stići lozinka."]
      (button-link "/" "Početna stranica" "btn btn-primary"))))

(defn navbar
  [email]
  [:nav {:class "navbar navbar-inverse"}
   [:div {:class "container-fluid"}
    [:div {:class "navbar-header"} [:a {:class "navbar-brand" :href "/"} "Formlogic"]]
    [:ul {:class "nav navbar-nav"}
     [:li (elem/link-to "/" "Home")]
     [:li (elem/link-to "/" "Else")]]
    [:ul {:class "nav navbar-nav navbar-right"}
     [:li [:p {:class "navbar-text"} [:span {:class "glyphicon glyphicon-user"}] " " email]]
     [:li (elem/link-to "/logout" [:span {:class "glyphicon glyphicon-log-out"}] " Odjava")]]]])

(defn panel-column
  [panel-class title & contents]
  [:div {:class "col-lg-6"}
   [:div {:class (str "panel " panel-class)}
    [:div {:class "panel-heading"} title]
    [:div {:class "panel-body"} contents]]])

(defn- question-label
  [{:keys [id ord body]}]
  (label {:class "control-label" :for (str "question-" id)} "question"
         (str ord ". " body)))

(defn multiple-choice-question
  [question]
  (let [radio? (= (:type question) "single")
        elem-class (if radio? "radio" "checkbox")
        elem-id (fn [idx] (str "question" (:id question) (when-not radio? (str "-option-" idx))))
        elem-factory (if radio? radio-button check-box)]
  [:div {:class "form-group"}
   (question-label question)
   (for [choice (map-indexed vector (:choices question))
         :let [[idx text] choice]]
     [:div {:class elem-class}
      [:label (elem-factory (elem-id idx) false (h text)) (h text)]])]))

(defn fill-question
  [{id :id :as question}]
    [:div {:class "form-group"}
     (question-label question)
     (text-area {:class "form-control" :rows "3"} (str "question" id "-fill"))])

(defn render-question
  [question]
  (case (:type question)
    ("single" "multiple") (multiple-choice-question question)
    "fill" (fill-question question)))

(defn user-page
  [user]
  )

(defn task-page
  [session]
  (let [user (:user session)]
    (page-template
      "Pitanja"
      (navbar (:email user))
      [:h1 {:class "text-center"} "Zadatak 1"]
      [:h3 "Stranica 2"]
      [:div {:class "row"}
       (panel-column "panel-default" "Demonstracija")
       (panel-column "panel-primary" "Pitanja"
                     [:form {:name "taskForm"
                             :novalidate ""
                             :role "form"
                             :method "post"
                             :action "/user/answers"}
                      (anti-forgery-field)
                      (render-question {:ord 1 :type "fill" :body "Koje je boje nebo?" :id 1})
                      (render-question {:ord 2 :type "multiple" :body "Koje od ponuđenih osoba se bave, ili su se bavile, glumom?" :id 2 :choices ["Frenk Sinatra" "Teodor Ruzvelt" "Džon Travolta" "Madona"]})
                      (render-question {:ord 3 :type "single" :body "Koliko postoji kontinenata na zemaljskoj kugli?" :id 3 :choices ["dva" "nijedan" "pet" "osam"]})
                     [:button {:class "btn btn-primary btn-block" :type "submit"} "Sledeće"]])
       ])))
