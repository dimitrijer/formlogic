(ns formlogic.views
  (:require [hiccup.element :as elem]
            [clojure.tools.logging :as log]
            [clojure.string :as str]
            [formlogic.db :as db])
  (:use [hiccup.page :only (html5 include-css include-js)]
        [hiccup.core :only (h html)]
        [hiccup.form]
        [ring.util.anti-forgery]))

(defn- page-template
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
          (include-js "/js/ui-bootstrap-tpls-2.1.4.min.js")
          (include-js "/js/script.js")
          (include-js "/js/katex.min.js")
          (include-css "/css/style.css")
          (include-css "/css/katex.min.css")
          (include-css "/css/bootstrap.min.css")
          [:body [:div {:class "container"} contents]]]))

(defn- button-link
  ([uri btn-label] (button-link uri btn-label "btn btn-primary"))
  ([uri btn-label btn-class]
   (elem/link-to {:class btn-class} uri btn-label)))

(defn- well
  [& contents]
  [:div {:class "im-centered well"} contents])

(defn- wide-well
  [& contents]
  [:div {:class "im-centered-wide well"} contents])

(defn- ng-alert
  [alert-class div-args span-args & contents]
  [:div (into {:class (str "alert " alert-class) :role "alert"} div-args)
   [:span {:class "col-lg-1 glyphicon glyphicon-alert" :aria-hidden "true"}]
   [:span (into {:class "col-lg-11"} span-args) contents]])

(def ^{:private true} date-format (java.text.SimpleDateFormat. "HH:mm:ss dd/MM/YYYY"))

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

(defn- navbar
  [{admin? :admin email :email}]
  [:nav {:class "navbar navbar-inverse"}
   [:div {:class "container-fluid"}
    [:div {:class "navbar-header"} [:a {:class "navbar-brand" :href "/"} "Formlogic"]]
    [:ul {:class "nav navbar-nav"}
     [:li (elem/link-to "/" "Početna")]]
    [:ul {:class "nav navbar-nav navbar-right"}
     [:li [:p {:class "navbar-text"} [:span {:class (if-not admin?
                                                      "glyphicon glyphicon-user"
                                                      "glyphicon glyphicon-star")}]
           (str " " email (when admin? " (administrator)"))]]
     [:li (elem/link-to "/user/logout"
                        [:span {:class "glyphicon glyphicon-log-out"}] " Odjava")]]]])

(defn- panel-column
  [panel-class div-args title & contents]
  [:div div-args
   [:div {:class (str "panel " panel-class)}
    [:div {:class "panel-heading"} title]
    [:div {:class "panel-body"} contents]]])

(defn- question-label
  [{:keys [id ord body]}]
  (label {:class "control-label" :for (str "question-" id)} "question"
         (str ord ". " body)))

(defn- correct-radio-group [question]
  [:div {:class "btn-group"}
   [:hr]
   [:label {:class "btn btn-success btn-outline"
            :ng-model (str "question" (:id question) "correct")
            :uib-btn-radio "true"} [:span {:class "glyphicon glyphicon-ok"}] " Tačno"]
   [:label {:class "btn btn-danger btn-outline"
            :ng-model (str "question" (:id question) "correct")
            :uib-btn-radio "false"} [:span {:class "glyphicon glyphicon-remove"}] "Netačno"]])

(defn- multiple-choice-question
  [{admin? :admin} question progress]
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
        [:label (elem-factory (merge {} (when admin? {:disabled "true"}))
                              (elem-id idx)
                              (active? text)
                              (h text)) (h text)]])
     (when admin? (correct-radio-group question))]))

(defn- fill-question
  [{admin? :admin} {id :id :as question} progress]
  [:div {:class "form-group"}
   (question-label question)
   (text-area (merge {:class "form-control" :rows "3"}
                     (when admin? {:disabled "true"}))
              (str "question" id "-fill")
              (first (:answers progress)))
   (when admin? (correct-radio-group question))])

(defn- render-question
  [user {:keys [question progress]}]
  [:div
   (when (:admin user)
     (let [correct-elem-id (str "question" (:id question) "correct")]
       ;; This hidden field will be sent along with the rest of form parameters
       ;; on submit. Its value is bound to the same Angular model as correct
       ;; radio group.
       [:input {:type "hidden"
                :name correct-elem-id
                :ng-init (str correct-elem-id " = " (:correct progress))
                :value (str "{{ " correct-elem-id " }}")}]))
   (case (:type question)
     ("single" "multiple") (multiple-choice-question user question progress)
     "fill" (fill-question user question progress))])

(defn- calculate-perc-completed
  [assignment-progress-id assignment-id]
  (let [questions-completed (:count
                              (db/unique-result
                                db/get-number-of-questions-completed-for-progress
                                {:id assignment-progress-id}))
        total-questions (:count
                          (db/unique-result
                            db/get-number-of-questions-for-assignment
                            {:id assignment-id}))
        correct-questions (:count (db/unique-result
                                    db/get-number-of-correct-questions-for-progress
                                    {:id assignment-progress-id}))]
    {:progress (int (* 100 (/ questions-completed (double total-questions))))
     :grade (int (* 100 (/ correct-questions (double total-questions))))}))

(defn- render-assignment
  [user assignment]
  (let [assignment-id (:id assignment)
        tasks (:count (db/unique-result db/get-number-of-tasks-for-assignment
                                        {:id assignment-id}))
        questions (:count (db/unique-result db/get-number-of-questions-for-assignment
                                            {:id assignment-id}))
        assignment-progress (db/unique-result db/find-progress-by-assignment-id
                                              {:assignment_id assignment-id
                                               :user_id (:id user)})
        in-progress? (not (nil? assignment-progress))
        completed? (and in-progress? (:completed_at assignment-progress))
        {:keys [progress grade]} (if in-progress?
                                   (calculate-perc-completed
                                     (:id assignment-progress)
                                     assignment-id)
                                   {:progress 0 :grade 0})
        passed? (> grade 50)]
    [:tr
     (if completed?
       {:class "success"}
       (when in-progress?
         {:class "warning"}))
     [:td (:name assignment)]
     [:td tasks]
     [:td questions]
     [:td (if in-progress?
            (.format date-format (:started_at assignment-progress))
            "Nije započet")]
     [:td (if completed?
            (str "Završen u " (.format date-format (:completed_at assignment-progress)))
            [:uib-progressbar {:type "default"
                               :animate "false"
                               :value progress}
             (when in-progress? (str progress "%"))])]
     [:td (if-not completed?
            (button-link (str "/user/progress/" (:id assignment))
                         "Polaži"
                         "btn btn-default")
            [:strong
             (when-not passed? {:style "color: #ff0000;"})
             (str (if passed? "Položen" "Nije položen") " (" grade "%)")])]]))

(defn assignments-page
  [user]
  (page-template
    "Zadaci"
    (navbar user)
    [:h1 "Zadaci"]
    [:uib-accordion
     (for [category (db/load-assignment-categories)
           :let [category-name (:category category)
                 cnt (:cnt category)]]
       [:div {:class "panel-primary" :uib-accordion-group ""}
        [:uib-accordion-heading
         (h category-name) [:span {:class "pull-right badge"} cnt]]
        [:table {:class "table table-hover"}
         [:thead [:tr
                  [:th "Ime"]
                  [:th "Stranica"]
                  [:th "Pitanja"]
                  [:th "Započet"]
                  [:th "Progres"]
                  [:th "Ocena"]]]
         [:tbody
          (for [assignment (db/load-assignments-by-category {:category category-name})]
            (render-assignment user assignment))]]])]))

(defn- modal-template-ok
  [id title ok-label cancel-label & contents]
  [:script {:type "text/ng-template"
            :id id}
   [:div {:class "modal-header"}
    [:h3 {:class "modal-title" :id "modal-title"} title]]
   [:div {:class "modal-body" :id "modal-body"} contents]
   [:div {:class "modal-footer"}
    [:button {:class "btn btn-danger"
              :type "button"
              :ng-click "$ctrl.ok()"} ok-label]
    [:button {:class "btn btn-default"
              :type "button"
              :ng-click "$ctrl.cancel()"} cancel-label]]])

(defn- task-demonstration-panel [task]
  (panel-column "panel-default"
                {:class "col-lg-6"}
                "Demonstracija"
                ;; Eval magic right here.
                (eval (read-string (:contents task)))))

(defn task-page
  [user assignment-progress {:keys [task questions]}]
  {:pre [task]}
  (let [admin? (:admin user)
        only-questions? (nil? (:contents task))
        first-task? (= 1 (:ord task))
        assignment-id (get-in assignment-progress [:assignment :id])
        total-tasks (:count (db/unique-result db/get-number-of-tasks-for-assignment
                                              {:id assignment-id}))
        last-task? (= total-tasks (:ord task))
        perc-complete (:progress (calculate-perc-completed (:id assignment-progress)
                                                           assignment-id))]
    (page-template
      "Pitanja"
      (include-js "/js/task.js")
      (navbar user)
      [:h1 {:class "text-center"} (get-in assignment-progress [:assignment :name])]
      [:h3 {:class "text-center"} (str "Stranica " (:ord task) " od " total-tasks)]
      [:div {:class "row"}
       [:div {:class "col-lg-offset-4 col-lg-4"}
        [:uib-progressbar {:id "progress"
                           :type "success"
                           :animate "false"
                           :value perc-complete} (str perc-complete "%")]]]
      (when admin? [:h5 (str "Student: " (get-in assignment-progress [:user :email]))])
      [:div {:class "row"}
       ;; Demonstration panel.
       (when-not only-questions? (task-demonstration-panel task))
       ;; Questions panel.
       (panel-column "panel-primary"
                     {:class (str (when only-questions? "col-lg-offset-3 ")
                                  "col-lg-6")}
                     "Pitanja"
                     [:form {:id "taskForm"
                             :name "taskForm"
                             :novalidate ""
                             :role "form"
                             :method "post"
                             :ng-controller "TaskFormController as $ctrl"
                             :data-action-uri (str "/user/progress/"
                                                   assignment-id
                                                   "/"
                                                   (:ord task))
                             :action "#"}
                      (when (and (not admin?) last-task?)
                        (modal-template-ok "ok-modal.html"
                                           "Potvrdite predaju"
                                           "Predaj"
                                           "Nazad"
                                           [:p "Da li ste " [:strong "sigurni"] " da želite da predate test? Ova akcija se ne može poništiti."]))
                      (anti-forgery-field)
                      (for [question questions]
                        (render-question user question))
                      [:div {:class "row"}
                       (when-not first-task?
                         [:button {:class "col-lg-offset-2 col-lg-3 btn btn-default"
                                   :type "button"
                                   :ng-click "onSubmitPrev()"}
                          "Nazad"])
                       [:button {:class (str (if first-task?
                                               "col-lg-offset-7 "
                                               "col-lg-offset-2 ")
                                             (if last-task?
                                               "btn-danger "
                                               "btn-primary ")
                                             "col-lg-3 btn")
                                 :type "button"
                                 :ng-click (if (and (not admin?) last-task?) "openOkModal()" "onSubmitNext()")}
                        (if last-task?  (if-not admin? "Predaj" "Kraj") "Dalje")]]])])))

(defn- student-progress-tab
  []
  [:div {:class "tab-group"}
   [:div {:class "form-group"}
    [:input {:type "text"
             :ng-model "student"
             :placeholder "Email studenta"
             :ng-model-options "{ debounce: 250 }"
             :uib-typeahead "student as student.email for student in getStudents($viewValue)"
             :typeahead-loading "loadingStudents"
             :typeahead-on-select "onStudentSelected($item)"
             :typeahead-no-results "noStudent"
             :class "form-control"}
     [:i {:ng-show "loadingStudents" :class "glyphicon glyphicon-refresh"}]
     [:div {:ng-show "noStudent"}
      [:i {:class "glyphicon glyphicon-remove"}] "Ne postoji takav nalog"]]]
   [:div {:ng-show "studentProgresses.length == 0"} [:strong "Student nema završenih testova."]]
   [:table {:ng-show "studentProgresses.length > 0"
            :class "table table-hover"}
    [:thead
     [:th "Test"]
     [:th "Započet"]
     [:th "Završen"]
     [:th "Ocena"]
     [:th "Link"]]
    [:tbody
     [:tr {:ng-repeat "progress in studentProgresses"}
      [:td "{{ progress.name }}"]
      [:td "{{ progress.started_at }}"]
      [:td "{{ progress.completed_at }}"]
      [:td [:strong {:ng-class "{ red: progress.grade < 51 }"} "{{ progress.grade }}%"]]
      [:td [:a {:href "/user/progresses/{{ progress.id }}"}
            [:button {:class "btn btn-primary"
                      :type "button"} "Pregledaj"]]]]]]])

(defn- assignment-progress-tab
  []
  [:div {:class "tab-group"}
   [:div {:class "form-group"}
    [:input {:type "text"
             :ng-model "assignment"
             :placeholder "Ime testa"
             :ng-model-options "{ debounce: 250 }"
             :uib-typeahead "test as test.name for test in getTests($viewValue)"
             :typeahead-loading "loadingTests"
             :typeahead-on-select "onTestSelected($item)"
             :typeahead-no-results "noTests"
             :class "form-control"}
     [:i {:ng-show "loadingTests" :class "glyphicon glyphicon-refresh"}]
     [:div {:ng-show "noTests"}
      [:i {:class "glyphicon glyphicon-remove"}] "Ne postoji test sa tim imenom"]]]
   [:div {:ng-show "assignmentProgresses.length == 0"} [:strong "Ne postoje završene instance testa."]]
   [:table {:ng-show "assignmentProgresses.length > 0"
            :class "table table-hover"}
    [:thead
     [:th "Student"]
     [:th "Započet"]
     [:th "Završen"]
     [:th "Ocena"]
     [:th "Link"]]
    [:tbody
     [:tr {:ng-repeat "progress in assignmentProgresses"}
      [:td "{{ progress.email }}"]
      [:td "{{ progress.started_at }}"]
      [:td "{{ progress.completed_at }}"]
      [:td [:strong {:ng-class "{ red: progress.grade < 51 }"} "{{ progress.grade }}%"]]
      [:td [:a {:href "/user/progresses/{{ progress.id }}"}
            [:button {:class "btn btn-primary"
                      :type "button"} "Pregledaj"]]]]]]])

(defn administrator-page
  [user]
  (page-template
    "Početna"
    (include-js "/js/admin.js")
    (navbar user)
    [:h1 "Pregled testova"]
    [:div {:ng-controller "TypeaheadCtrl"}
     [:uib-tabset
      [:uib-tab {:index "0" :heading "Po studentima"} (student-progress-tab)]
      [:uib-tab {:index "1" :heading "Po testovima"} (assignment-progress-tab)]]]))

(defn cnf-page []
  (let [result 
    (vector :div {:style "margin: 20px;"}
        [:script {:type "text/javascript" :src "/js/latex.js"}]
        [:h2 "Svođenje na KNF"]
        [:p "Unesite formulu:"]
        [:div {:class "form-group"
               :ng-controller "LatexFormController"}
         (anti-forgery-field)
         [:div {:class "row"} [:textarea {:type "text"
                                           :rows 3
                                           :ng-model "formula"
                                           :placeholder "Formula"
                                           :class "form-control"}]]
          [:div {:style "margin-top: 10px; " :class "row"} [:button {:class "btn btn-primary"
                    :ng-click "sendFormula()"
                    :type "button"} "Pošalji"]]
         [:div {:style "margin-top: 20px;" :class "panel"}
          [:div {:class "well"
                 :ng-show "error != null"}
           [:p {:class "red" :style "white-space: pre;"} "{{ error }}"]]
          [:div {:class "row"
                 :style "overflow: auto;"
                 :ng-show "error == null"}
           [:h4 "0. Početna formula"]
           [:span {:id "katexEquation0" :class "katex"}]]
          [:div {:class "row"
                 :style "overflow: auto;"
                 :ng-show "error == null"}
           [:h4 "1. Eliminisanje implikacija"]
           [:span {:id "katexEquation1" :class "katex"}]]
          [:div {:class "row"
                 :style "overflow: auto;"
                 :ng-show "error == null"}
           [:h4 "2. Spuštanje negacija do atomskog nivoa"]
           [:span {:id "katexEquation2" :class "katex"}]]
          [:div {:class "row"
                 :style "overflow: auto;"
                 :ng-show "error == null"}
           [:h4 "3. Zamena egzistencijalnih kvant. f-jama"]
           [:span {:id "katexEquation3" :class "katex"}]]
          [:div {:class "row"
                 :style "overflow: auto;"
                 :ng-show "error == null"}
           [:h4 "4. Preimenovanje varijabli"]
           [:span {:id "katexEquation4" :class "katex"}]]
          [:div {:class "row"
                 :style "overflow: auto;"
                 :ng-show "error == null"}
           [:h4 "5. Premeštanje univerzalnih kvant. na početak"]
           [:span {:id "katexEquation5" :class "katex"}]]
          [:div {:class "row"
                 :style "overflow: auto;"
                 :ng-show "error == null"}
           [:h4 "6. Spuštanje disjunkcija do atomskog nivoa"]
           [:span {:id "katexEquation6" :class "katex"}]]
          [:div {:class "row"
                 :style "overflow: auto;"
                 :ng-show "error == null"}
           [:h4 "7. Preimenovanje varijabli (ponovo)"]
           [:span {:id "katexEquation7" :class "katex"}]]
          ]])]
    
    (log/spy result)
    result))
