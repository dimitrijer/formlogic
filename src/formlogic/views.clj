(ns formlogic.views
  (:use [hiccup.core :only (html)]
        [hiccup.page :only (include-css include-js)]))

(defn page
  "Returns HTML page with provided title and contents."
  [title & contents]
  (html {:lang "rs"}
        [:title title]
        (include-css "css/style.css")
        (include-js "js/main.js")
        [:body [:div {:class "container"} contents]]))

