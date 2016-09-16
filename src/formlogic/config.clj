(ns formlogic.config
  (require [immuconf.config :as conf]))

(def cfg (conf/load "resources/config.edn"))

(def cget (partial conf/get cfg))
