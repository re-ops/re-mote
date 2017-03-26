(comment
   re-mote, Copyright 2017 Ronen Narkis, narkisr.com
   Licensed under the Apache License,
   Version 2.0  (the "License") you may not use this file except in compliance with the License.
   You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.)

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

