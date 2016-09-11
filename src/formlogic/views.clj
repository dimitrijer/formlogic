(ns formlogic.views
  (:use [hiccup.page :only (html5 include-css include-js)]))

(defn page
  "Returns HTML page with provided title and contents."
  [title & contents]
  (html5 {:lang "rs" :ng-app "myApp"}
         [:title title]
         (include-css "css/bootstrap.min.css")
         (include-js "js/angular.min.js")
         (include-js "js/ui-bootstrap-tpls-2.1.3.min.js")
         (include-js "js/script.js")
         [:body [:div {:class "container"} contents]]))

(def home-page (page "Home" [:div {:id "content"} [:h1 {:class "text-success"} "Hello, Hiccup!"]]))
