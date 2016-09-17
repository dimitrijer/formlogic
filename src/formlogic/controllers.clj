(ns formlogic.controllers
  (require [ring.util.http-response :as resp]
           [postal.core :as postal]
           [clojure.tools.logging :as log]
           [clojure.string :as str]
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

(defn login [email password]
  (if (validate-email email)
    (if-let [user (db/unique-result db/find-user-by-email {:email (str/lower-case email)})]
      (do
        (log/debug "User" email "logged in.")
        (resp/internal-server-error "amigaaaawd"))
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
      ;; TODO validate email
      (if existing-user
        (views/register-page :alert ["Korisnik " [:strong email] " je već registrovan!"])
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
