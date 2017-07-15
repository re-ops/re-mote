(ns re-mote.publish.index
  (:require
   [hiccup.core :refer [html]]
   [hiccup.page :refer [html5 include-js include-css]]))

(defn index []
  (html
   (html5
    [:head
     [:title "re-mote"]
     [:meta {:charset "utf-8"}]
     [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge"}]
     [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
     (include-css "/assets/bootstrap/css/bootstrap.min.css")]
    [:body
     [:div.container [:div#app.app-wrapper]]
     (include-js "js/main.js")])))

