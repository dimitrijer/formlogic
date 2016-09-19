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
                  (.getArray v)
                  v)])))

(defn get-or-create-progress
  [user assignment-id]
  (let [query-params {:user_id (:id user)
                      :assignment_id (Integer/parseInt assignment-id)}
        assignment-progress (unique-result find-progress-by-assignment-id
                                           query-params)]
    (if-not assignment-progress
      (do
        ;; Create new progress.
        (log/debug "User" (:email user)
                   "started progress of assignment" assignment-id)
        (insert-progress! query-params))
      (do
        (log/debug "User" (:email user)
                   "continued progress ID" (:id assignment-progress)
                   "of assignment" assignment-id)
        assignment-progress))))
