(ns formlogic.controllers
  (require [ring.util.http-response :as resp]
           [postal.core :as postal]
           [clojure.tools.logging :as log]
           [clojure.string :as str]
           [formlogic.config :refer [cget]]
           [formlogic.parser :refer [char-range]]
           [formlogic.views :as views]
           [formlogic.db :as db]))

(defn login [email password]
  (resp/internal-server-error "amigaaaawd"))

(def password-alphabet (concat (char-range \a \z)
                               (char-range \A \Z)
                               (char-range \0 \9)))

(defn generate-password
  [length]
  (apply str (take length (repeatedly #(rand-nth password-alphabet)))))

(def password-length 10)

(defn register [email]
  (let [password (generate-password password-length)
        email (if (cget :production) email "templaryum@gmail.com")]
    (db/insert-user! {:email email :password password})
    (log/debug "User" email "registered successfully.")
    (postal/send-message {:from "admin@formlogic.etf.rs"
                          :to email
                          :subject "Lozinka za formlogic"
                          :body (str "Vaša lozinka je: " password)})
    (views/register-success email)))
