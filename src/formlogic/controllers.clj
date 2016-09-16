(ns formlogic.controllers
  (require [ring.util.http-response :as resp]))

(defn login [params]
  (let [{:keys [email password]} params]
    (resp/internal-server-error "amigaaaawd")))
