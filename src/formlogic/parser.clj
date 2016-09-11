(ns formlogic.parser
  (:require [instaparse.core :as insta]))

(insta/parser (clojure.java.io/resource "logic.bnf") :auto-whitespace :standard)
