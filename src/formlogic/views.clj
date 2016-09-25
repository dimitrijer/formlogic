(ns formlogic.views
  (:require [hiccup.element :as elem]
            [clojure.tools.logging :as log]
            [clojure.string :as str]
            [formlogic.db :as db])
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
      [:p "Pristup resursu je onemogućen usled nedovoljnih privilegija. Molimo
          ulogujte se ponovo."]
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
     [:li (elem/link-to "/user/logout" [:span {:class "glyphicon glyphicon-log-out"}] " Odjava")]]]])

(defn panel-column
  [panel-class div-args title & contents]
  [:div div-args
   [:div {:class (str "panel " panel-class)}
    [:div {:class "panel-heading"} title]
    [:div {:class "panel-body"} contents]]])

(defn- question-label
  [{:keys [id ord body]}]
  (label {:class "control-label" :for (str "question-" id)} "question"
         (str ord ". " body)))

(defn multiple-choice-question
  [question progress]
  (let [radio? (= (:type question) "single")
        elem-class (if radio? "radio" "checkbox")
        elem-id (fn [idx] (str "question"
                               (:id question)
                               (when-not radio? (str "-option-" idx))))
        elem-factory (if radio? radio-button check-box)
        active? (fn [choice] (some #{choice} (:answers progress)))]
  [:div {:class "form-group"}
   (question-label question)
   (for [choice (map-indexed vector (:choices question))
         :let [[idx text] choice]]
     [:div {:class elem-class}
      [:label (elem-factory (elem-id idx) (active? text) (h text)) (h text)]])]))

(defn fill-question
  [{id :id :as question} progress]
  [:div {:class "form-group"}
   (question-label question)
   (text-area {:class "form-control" :rows "3"}
              (str "question" id "-fill")
              (first (:answers progress)))])

(defn render-question
  [{:keys [question progress]}]
  (case (:type question)
    ("single" "multiple") (multiple-choice-question question progress)
    "fill" (fill-question question progress)))

(defn user-page
  [user]
  (page-template
    "Zadaci"
    (navbar (:email user))
    [:h1 "Zadaci"]
    [:div {:class "panel-group"}
     (for [category (db/load-assignment-categories)
           :let [category-name (:category category)
                 cnt (:cnt category)]]
       [:div {:class "panel panel-default"}
        [:div {:class "panel-heading"}
         [:h4 {:class "panel-title" } (h category-name) [:span {:class "pull-right badge"} cnt]]]
        [:div {:class "list-group"}
         (for [{assignment-id :id
                assignment-name :name} (db/load-assignments-by-category {:category category-name})]
           [:a {:href (str "/user/progress/" assignment-id) :class "list-group-item"}
            assignment-name])]])]))

(defn- calculate-perc-completed
  [assignment-progress-id assignment-id]
  (let [questions-completed (:count)
        (db/unique-result db/get-questions-completed-for-progress
                          {:id (:id assignment-progress)})
        total-questions (:count
                          (db/unique-result db/get-number-of-questions-for-assignment
                                            {:id assignment-id}))]
    (int (* 100 (/ questions-completed (double total-questions))))))

(defn task-page
  [user assignment-progress {:keys [task questions]}]
  (let [only-questions? (nil? (:contents task))
        first-task? (= 1 (:ord task))
        assignment-id (get-in assignment-progress [:assignment :id])
        perc-complete (calculate-perc-completed (:id assignment-progress))]
    (log/spy questions-completed)
    (log/spy total-questions)
    (log/spy perc-complete)
    (page-template
      "Pitanja"
      (navbar (:email user))
      [:h1 {:class "text-center"} (get-in assignment-progress [:assignment :name])]
      [:h3 {:class "text-center"} (str "Stranica " (:ord task))]
      [:div {:class "progress" :style "width: 30%;"}
       [:div {:class "progress-bar"
              :role "progressbar"
              :aria-valuemin "0"
              :aria-valuemax "100"
              :aria-valuenow perc-complete}
        (str perc-complete "%")]]
      [:div {:class "row"}
       (when-not only-questions?
         (panel-column "panel-default"
                       {:class "col-lg-6"}
                       "Demonstracija"
                       ;; Eval magic right here.
                       (eval (read-string (:contents task)))))
       (panel-column "panel-primary"
                     {:class (str (when only-questions? "col-lg-offset-3 ")
                                  "col-lg-6")}
                     "Pitanja"
                     [:form {:name "taskForm"
                             :novalidate ""
                             :role "form"
                             :method "post"
                             :action (str "/user/progress/"
                                          assignment-id
                                          "/"
                                          (:ord task))}
                      (anti-forgery-field)
                      (for [question questions]
                        (render-question question))
                      [:div {:class "row"}
                       (when-not first-task?
                         (button-link (str "/user/progress/"
                                           assignment-id
                                           "/"
                                           (dec (:ord task)))
                                      "Nazad"
                                      "col-lg-offset-1 col-lg-3 btn btn-default"))
                       [:button {:class (str (if first-task?
                                               "col-lg-offset-7"
                                               "col-lg-offset-3")
                                             " col-lg-3 btn btn-primary")
                                 :type "submit"} (h "Sačuvaj & Dalje")]]])])))
