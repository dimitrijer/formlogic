(ns formlogic.db
  (:require [yesql.core :refer [defqueries]]))

(def db-spec {:classname "org.postgresql.Driver"
              :subprotocol "postgresql"
              :subname "//localhost:5432/formlogic"
              :user "formlogic"})

(defqueries "sql/get.sql" {:connection db-spec})
(defqueries "sql/insert.sql" {:connection db-spec})
