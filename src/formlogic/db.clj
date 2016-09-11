(ns formlogic.db
  (:require [yesql.core :refer [defqueries]]))

(def db-spec {:classname "org.postgresql.Driver"
              :subprotocol "postgresql"
              :subname "//localhost:5432/formlogic"
              :user "formlogic"})

(defqueries "sql/get.sql" {:connection db-spec})

(find-user-by-id {:id 1})
(find-user-by-email {:email "dimitrijer@gmail.com"})
