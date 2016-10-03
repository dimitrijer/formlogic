;; db.clj
(defn unwrap-arrays
  "Unwraps Jdbc4Array instances into arrays. Needs to be called
  within a transaction."
  [m]
  (into {} (for [[k v] m]
             [k (if (instance? org.postgresql.jdbc4.Jdbc4Array v)
                  (vec (.getArray v))
                  v)])))

;; When calling db functions, pass it as :row-fn
(db/find-questions-by-task-id {:task_id 1}
                              {:row-fn db/unwrap-arrays})
