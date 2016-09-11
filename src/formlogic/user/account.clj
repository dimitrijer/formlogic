(ns formlogic.user.account
  (:require [postal.core :as postal]))

(defn register-user []
  (postal/send-message {:from "admin@formlogic.etf.rs"
                        :to "templaryum@gmail.com"
                        :subject "Lozinka za formlogic"
                        :body "Test"}))
