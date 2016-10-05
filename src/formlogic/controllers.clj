(ns formlogic.controllers
  (require [ring.util.http-response :as resp]
           [postal.core :as postal]
           [clojure.tools.logging :as log]
           [clojure.string :as str]
           [clojure.java.jdbc :as jdbc]
           [instaparse.core :as insta]
           [cheshire.core :as json]
           [formlogic.config :refer [cget]]
           [formlogic.parser :as parser]
           [formlogic.views :as views]
           [formlogic.renderer :as renderer]
           [formlogic.db :as db])
  (use [hiccup.core :only (h html)]))

(defn- validate-email
  "Oh, the horror..."
  [email]
  (let [pattern #"[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?"]
    (and (string? email) (re-matches pattern email))))

(defn login
  "Tries to login user with provided credentials. If succcessful, attaches user
  to the session."
  [email password-md5 {session :session}]
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
            (log/warn "User" email "tried to login with wrong password.")
            (resp/forbidden {:alert (html "Pogrešna lozinka!")}))))
      (do
        (log/warn "User" email "tried to login, but is not registered.")
        (resp/forbidden {:alert (html "Korisnik " [:strong email] " nije registrovan!")})))
    (do
      (log/error "User" email "tried to login, but does not match email pattern!")
      (resp/forbidden {:alert "Navedeni email je pogrešnog formata!"}))))

(def ^:private password-alphabet (concat (parser/char-range \a \z)
                                         (parser/char-range \A \Z)
                                         (parser/char-range \0 \9)))

(def ^:private password-length 10)

(defn- generate-password
  [length]
  (apply str (take length (repeatedly #(rand-nth password-alphabet)))))

(defn register
  "Registers a user with provided email account by sending an email with random
  password to provided email."
  [email]
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
  "De-attaches user from session on logout."
  [session]
  (if-let [user (:user session)]
    (do
      (log/debug "User" (:email user) "logged out.")
      (-> (resp/found "/login") (assoc :session nil)))
    (do
      (log/error "Tried to log out, but user is not logged in yet!")
      (views/login-page))))

(defn- attach-progress-to-session
  [session assignment-progress]
  (let [assignment-id (:assignment_id assignment-progress)
        assignment (db/unique-result db/find-assignment-by-id
                                     {:id assignment-id})
        ;; Note that this may not be the user from session, in admin case.
        user (db/unique-result db/find-user-by-id
                               {:id (:user_id assignment-progress)})
        session (-> session
                    (assoc-in [:progress assignment-id] assignment-progress)
                    (assoc-in [:progress assignment-id :assignment] assignment)
                    (assoc-in [:progress assignment-id :user] user))]
    ;; Attach progress to progress map, within session. Always start from first task.
    (log/debug (:email (:user session)) "attached progress ID" (:id assignment-progress)
               "from user" (:emal user))
    (-> (resp/found (str "/user/progress/" assignment-id "/" 1))
        (assoc :session session))))

(defn attach-progress
  "First form can only be invoked by admin and is used to attach some student's
  progress to admin's session. Second form is invoked by student sessions."
  ([session progress-id]
   {:pre [(:admin (:user session))]}
   (if-let [assignment-progress (db/unique-result db/find-progress-by-id
                                                  {:id progress-id})]
     (attach-progress-to-session session assignment-progress)
     (do
       (log/warn (:email (:user session)) "tried to attach non-existing progress to admin session!")
       (-> (resp/not-found views/not-found-page)))))
  ([session user assignment-id]
   (let [assignment-progress (db/get-or-create-progress user assignment-id)]
     (attach-progress-to-session session assignment-progress))))

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
  "Fetches task stuff (along with progress) from DB and renders it on task page."
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
                              tx)
          ;; Get correct answers by sampling choices with indices specified in
          ;; (:answers question).
          correct-answers (reduce #(conj %1 (nth (:choices question) %2))
                                  #{}
                                  (:answers question))
          ;; Check if every given answer is contained in correct answers. Also,
          ;; check if number of answers given matches question type (fill - 1,
          ;; single - 1, multiple - size of answers).
          correct? (and (every? #(contains? correct-answers %) answers)
                        (= (count correct-answers) (count answers)))]
      (db/update-question-progress! {:id (:id question-progress)
                                     :answers answers
                                     :correct correct?}
                                    {:connection tx})
      (log/debug "Updated question" (:id question) "progress for user"
                 (:email user) "with answers:" answers))))

(defn- update-question-grading
  [assignment-progress question correct? tx]
  (when-not (nil? correct?)
    (db/update-question-progress-grading! {:question_id (:id question)
                                           :assignment_progress_id (:id assignment-progress)
                                           :correct (Boolean/parseBoolean correct?)}
                                          {:connection tx})
    (log/debug "Updated progress ID" (:id assignment-progress)
               "question" (:id question) "correctness to" correct?)))

(defn- extract-correct [question answers]
  (get answers (str "question" (:id question) "correct")))

(defn- update-task-progress [session assignment-progress questions answers tx]
  (if (:admin (:user session))
    ;; Update correct fields for questions.
    (doall (map #(update-question-grading assignment-progress
                                          %
                                          (extract-correct % answers)
                                          tx)
                questions))
    ;; Update question progress with answers.
    (do
      (doall (map #(update-question-progress
                     (:user session)
                     assignment-progress
                     %
                     (extract-answers % answers)
                     tx)
                  questions)))))
(defn save-task
  "Saves task progress to DB."
  [session assignment-id task-ord answers continue?]
  (when-let [assignment-progress (get-in session [:progress assignment-id])]
    (jdbc/with-db-transaction [tx db/db-spec]
      ;; Fetch task, questions and question progress for this page from DB.
      (let [questions (collect-questions assignment-progress task-ord tx)]
        (update-task-progress session assignment-progress questions answers tx)
        (let [user (:user session)
              next-task-ord (inc task-ord)
              prev-task-ord (dec task-ord)
              assignment-id (get-in assignment-progress [:assignment :id])
              next-task (db/unique-result db/find-task-by-assignment-id
                                          {:assignment_id assignment-id
                                           :ord next-task-ord}
                                          {:connection tx})]

          (if continue?
            (if next-task
              ;; Move on to next page.
              (resp/found (str "/user/progress/" assignment-id  "/" next-task-ord))
              (do
                ;; Update completed timestamp.
                (when-not (:admin user)
                  (db/update-completed-progress! {:id (:id assignment-progress)}
                                                 {:connection tx})
                  (log/debug "User" (:email user)
                             "completed assignment" assignment-id
                             "(progress" (:id assignment-progress) ")!"))
                ;; All done, move on to home page.
                ;; TODO clean progress from session
                (resp/found (str "/user"))))
            ;; Go back to previous page.
            (resp/found (str "/user/progress/" assignment-id  "/" prev-task-ord))))))))

(defn search-students
  [user email]
  (if (:admin user)
    (json/generate-string {:results (apply vector (db/get-students-like {:email email}))})
    (resp/forbidden)))

(defn search-assignments
  [user assignment-name]
  (if (:admin user)
    (json/generate-string {:results (apply vector (db/get-assignments-like {:name assignment-name}))})
    (resp/forbidden)))

(defn get-progresses-for-student
  [user student-id]
  (if (:admin user)
    (json/generate-string {:results (apply vector
                                           (db/find-completed-progresses-for-user
                                             {:id (Integer/parseInt student-id)}))}
                          {:date-format "HH:mm:ss dd/MM/yyyy"})
    (resp/forbidden)))

(defn get-progresses-for-assignment
  [user assignment-id]
  (if (:admin user)
    (json/generate-string {:results (apply vector
                                           (db/find-completed-progresses-for-assignment
                                             {:id (Integer/parseInt assignment-id)}))}
                          {:date-format "HH:mm:ss dd/MM/yyyy"})
    (resp/forbidden)))

(defn parse-logic-formula [formula]
  (try
    (let [tree (parser/logic-parser formula)]
      (if-not (insta/failure? tree)
        (let [step-zero (parser/simplify-tree tree)
              step-one (parser/transform-implications step-zero)
              step-two (parser/transform-negations step-one)
              step-three (parser/transform-existential-quantifiers step-two)
              step-four (parser/transform-universal-quantifiers step-three)
              step-five (parser/pull-quantifiers-up step-four)
              step-six (parser/descend-disjunctions step-five)
              step-seven (parser/split-on-conjunctions step-six)]
          (json/generate-string {:step0 (renderer/hiccup->latex step-zero)
                                 :step1 (renderer/hiccup->latex step-one)
                                 :step2 (renderer/hiccup->latex step-two)
                                 :step3 (renderer/hiccup->latex step-three)
                                 :step4 (renderer/hiccup->latex step-four)
                                 :step5 (renderer/hiccup->latex step-five)
                                 :step6 (renderer/hiccup->latex step-six)
                                 :step7 []}))
        (do
          (let [failure (str "Malformatted formula! " (pr-str (insta/get-failure tree)))]
          (log/error failure)
          (resp/bad-request {:error failure})))))
    (catch Exception e
      (log/error e "Failed to parse formula!")
      (resp/bad-request {:error "Failed!"}))))
