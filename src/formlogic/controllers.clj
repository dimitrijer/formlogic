(ns formlogic.controllers
  (require [ring.util.http-response :as resp]
           [postal.core :as postal]
           [clojure.tools.logging :as log]
           [clojure.string :as str]
           [clojure.java.jdbc :as jdbc]
           [formlogic.config :refer [cget]]
           [formlogic.parser :refer [char-range]]
           [formlogic.views :as views]
           [formlogic.db :as db])
  (use [hiccup.core :only (h html)]))

(defn validate-email
  "Oh, the horror..."
  [email]
  (let [pattern #"[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?"]
    (and (string? email) (re-matches pattern email))))

(defn login [email password-md5 {session :session :as req}]
  (if (validate-email email)
    ;; Pull user from DB.
    (if-let [user (db/unique-result db/find-user-by-email {:email (str/lower-case email)})]
      (do
        ;; Check if password hashes match.
        (if (= password-md5 (:password user))
          (let [session (assoc session :user user)]
            (log/debug "User" email "logged in.")
            (-> (resp/ok {:user-id (:id user)}) (assoc :session session)))
          (do
            (log/debug "User" email "tried to login with wrong password.")
            (resp/forbidden {:alert (html "Pogrešna lozinka!")}))))
      (do
        (log/debug "User" email "tried to login, but is not registered.")
        (resp/forbidden {:alert (html "Korisnik " [:strong email] " nije registrovan!")})))
    (do
      (log/error "User" email "tried to login, but does not match email pattern!")
      (resp/forbidden {:alert "Navedeni email je pogrešnog formata!"}))))

(def password-alphabet (concat (char-range \a \z)
                               (char-range \A \Z)
                               (char-range \0 \9)))

(defn generate-password
  [length]
  (apply str (take length (repeatedly #(rand-nth password-alphabet)))))

(def password-length 10)

(defn register [email]
  (if (validate-email email)
    (let [password (generate-password password-length)
          send-to (if (cget :production) email "templaryum@gmail.com")
          existing-user (db/unique-result db/find-user-by-email {:email (str/lower-case email)})]
      (if existing-user
        (views/register-page :alert (html "Korisnik " [:strong email] " je već registrovan!"))
        (do
          (db/insert-user! {:email (str/lower-case email) :password password})
          (log/debug "User" email "registered successfully.")
          (postal/send-message {:from "admin@formlogic.etf.rs"
                                :to send-to
                                :subject "Lozinka za formlogic"
                                :body (str "Vaša lozinka je: " password)})
          (views/register-success email))))
    (do
      (log/error "User" email "tried to register, but does not match email pattern!")
      (views/register-page :alert ["Navedeni email je pogrešnog formata!"]))))

(defn logout
  [session]
  (if-let [user (:user session)]
    (do
      (log/debug "User" (:email user) "logged out.")
      (-> (resp/found "/login") (assoc :session nil)))
    (do
      (log/error "Tried to log out, but user is not logged in yet!")
      (views/login-page))))

(defn attach-progress
  [session user assignment-id]
  (let [assignment-progress (db/get-or-create-progress user assignment-id)
        assignment (db/unique-result
                     db/find-assignment-by-id
                     {:id (Integer/parseInt assignment-id)})
        session (-> session
                    (assoc-in [:progress assignment-id] assignment-progress)
                    (assoc-in [:progress assignment-id :assignment] assignment))]
    ;; Attach progress to progress map, within session. Always start from first task.
    (-> (resp/found (str "/user/progress/" assignment-id "/" 1))
        (assoc :session session))))

(defn- collect-questions
  [assignment-progress task-ord tx]
  (let [assignment-id (:assignment_id assignment-progress)
        task (db/unique-result db/find-task-by-assignment-id
                               {:assignment_id assignment-id
                                :ord task-ord}
                               {:connection tx})]
    (db/find-questions-by-task-id {:task_id (:id task)}
                                  {:row-fn db/unwrap-arrays
                                   :connection tx})))

(defn- collect-task-progress
  [assignment-progress task-ord tx]
  (let [assignment-id (:assignment_id assignment-progress)
        task (db/unique-result db/find-task-by-assignment-id
                               {:assignment_id assignment-id
                                :ord task-ord}
                               {:connection tx})
        questions (db/find-questions-by-task-id {:task_id (:id task)}
                                                {:row-fn db/unwrap-arrays
                                                 :connection tx})
        questions-progress (db/find-question-progress-by-assignment-id-for-task
                             {:assignment_progress_id (:id assignment-progress)
                              :task_id (:id task)}
                             {:row-fn db/unwrap-arrays
                              :connection tx})]
    {:task task
     :questions (into [] (map
                           (fn [question] {:question question
                                           :progress (first (filter
                                                              #(= (:question_id %)
                                                                  (:id question))
                                                              questions-progress))})
                           questions))}))

(defn render-task
  [session assignment-id task-ord]
  (if-let [assignment-progress (get-in session [:progress assignment-id])]
    ;; Fetch task, questions and question progress for this page from DB.
    (jdbc/with-db-transaction [tx db/db-spec]
      (let [task-progress (collect-task-progress assignment-progress task-ord tx)]
        ;; Render task page with collected data.
        (views/task-page (:user session) assignment-progress task-progress)))
    (log/errorf (str "User %s tried to render task %s without attaching progress "
                     "for assignment %s!")
                (:email (:user session))
                task-ord
                assignment-id)))

(defn- extract-answers
  [question answers]
  "Returns a vector of strings that represent answers to the question."
  (case (:type question)
    "single" (vector (get answers (str "question" (:id question)) ""))
    "multiple" (first (filter (complement empty?)
                      [((comp vec vals select-keys) answers
                        (map-indexed
                          (fn
                            [idx item]
                            (str "question" (:id question) "-option-" idx))
                          (:choices question)))
                       [""]]))
    "fill" (vector (get answers (str "question" (:id question) "-fill") ""))))

(defn- update-question-progress
  [user assignment-progress question answers tx]
  (when-not (empty? answers)
    (let [question-progress (db/get-or-create-question-progress
                              user
                              (:id assignment-progress)
                              (:id question)
                              tx)]
      (db/update-question-progress! {:id (:id question-progress)
                                     :answers answers}
                                    {:connection tx})
      (log/debug "Updated question" (:id question) "progress for user"
                 (:email user) "with answers:" answers))))

(defn save-task
  [session assignment-id task-ord answers]
  (if-let [assignment-progress (get-in session [:progress assignment-id])]
    ;; Fetch task, questions and question progress for this page from DB.
    (jdbc/with-db-transaction [tx db/db-spec]
      (let [questions (collect-questions assignment-progress task-ord tx)]
        ;; Update question progress with answers.
        (doall (map #(update-question-progress
                (:user session)
                assignment-progress
                %
                (extract-answers % answers)
                tx)
             questions))
        (views/task-page (:user session)
                         assignment-progress
                         (collect-task-progress assignment-progress task-ord tx))))))
