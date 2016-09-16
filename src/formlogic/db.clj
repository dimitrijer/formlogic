(ns formlogic.db
  (:require [yesql.core :refer [defqueries]]
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
