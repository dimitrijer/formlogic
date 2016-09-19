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

(defn attach-progress
  [session user assignment-id]
  (let [assignment-progress (db/get-or-create-progress user assignment-id)
        session (assoc-in session [:progress assignment-id] assignment-progress)]
    ;; Attach progress to progress map, within session. Always start from first task.
    (-> (resp/found (str "/user/progress/" assignment-id "/" 1))
        (assoc :session session))))

(defn render-task
  [session assignment-id task-ord]
  (if-let [assignment-progress (get-in session [:progress assignment-id])]
    ;; Fetch task, questions and previous questions for this page from DB.
    (jdbc/with-db-transaction [tx db/db-spec]
      (let [assignment-id (:assignment_id assignment-progress)
            task (db/unique-result db/find-task-by-assignment-id
                                   {:assignment_id assignment-id
                                    :ord (Integer/parseInt task-ord)}
                                   {:connection tx})
            questions (db/find-questions-by-task-id {:task_id (:id task)}
                                                    {:connection tx})
            questions-progress (db/find-question-progress-by-assignment-id-for-task
                                 {:assignment_progress_id (:id assignment-progress)
                                  :task_id (:id task)}
                                 {:connection tx})]
        ;; Render task page with collected data, but first map question to its
        ;; progress.
        (views/task-page (:user session) task
                         (into [] (map
                                    (fn [question] {:question (db/unwrap-arrays question)
                                                    :progress (first (filter
                                                                       #(= (:question_id %)
                                                                           (:id question))
                                                                       questions-progress))})
                                    questions)))))
    (log/errorf (str "User %s tried to render task %s without attaching progress "
                     "for assignment %s!")
                (:email (:user session))
                task-ord
                assignment-id)))
