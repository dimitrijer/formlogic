(ns formlogic.db
  (:require [yesql.core :refer [defqueries]]
            [clojure.tools.logging :as log]
            [formlogic.config :refer [cget]]))

(def db-spec {:classname "org.postgresql.Driver"
              :subprotocol "postgresql"
              :subname (format "//%s:%d/%s"
                               (cget :db :host)
                               (cget :db :port)
                               (cget :db :database))
              :user (cget :db :user)})

(defqueries "sql/get.sql" {:connection db-spec})
(defqueries "sql/insert.sql" {:connection db-spec})
(defqueries "sql/update.sql" {:connection db-spec})

(defn unique-result
  ([query params]
   (unique-result query params {}))
  ([query params spec]
   (query params (assoc spec :result-set-fn first))))

(defn unwrap-arrays
  "Unwraps Jdbc4Array instances into arrays. Needs to be called within a TX."
  [m]
  (into {} (for [[k v] m]
             [k (if (instance? org.postgresql.jdbc4.Jdbc4Array v)
                  (vec (.getArray v))
                  v)])))

(defn get-or-create-progress
  [user assignment-id]
  (let [query-params {:user_id (:id user)
                      :assignment_id (Integer/parseInt assignment-id)}
        assignment-progress (unique-result find-progress-by-assignment-id
                                           query-params)]
    (if-not assignment-progress
      ;; Create new progress.
      (let [assignment-progress-id (insert-progress! query-params)]
        (log/debug "User" (:email user)
                   "started progress of assignment" assignment-id)
        (unique-result find-progress-by-assignment-id query-params))
      (do
        (log/debug "User" (:email user)
                   "continued progress ID" (:id assignment-progress)
                   "of assignment" assignment-id)
        assignment-progress))))

(defn get-or-create-question-progress
  [user assignment-progress-id question-id tx]
  (let [query-params {:assignment_progress_id assignment-progress-id
                      :question_id question-id}
        question-progress (unique-result find-question-progress-for-user
                                         query-params
                                         {:row-fn unwrap-arrays
                                          :connection tx})]
    (if-not question-progress
      (let [question-progress-id (insert-question-progress! query-params)]
        ;; Create new progress.
        (log/debug "Created question progress for question" question-id
                   "for user" (:email user))
        (unique-result find-question-progress-for-user
                       query-params
                       {:row-fn unwrap-arrays
                        :connection tx}))
      question-progress)))
