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

(ns supernal.publish.vega
  (:require 
    [supernal.publish.graphs :refer (graphs)]
    [cljs.reader :as reader]
    [clojure.pprint :as pprint]
    [promesa.core :as p]
    [reagent.core :as r]
    [taoensso.timbre :as timbre]
    [vega-tools.core :as vega-tools]))

(defonce app-state (r/atom {}))

(defn vega-chart [{:keys [chart]}]
  (r/create-class
   {:display-name "vega-chart"
    :reagent-render (fn [] [:div])
    :component-did-mount
    (fn [this]
      (.update (chart {:el (r/dom-node this)})))}))

(defn update-graph [{:keys [graph values]}]
    (let [{:keys [gname gtype]} graph]
        (swap! app-state assoc-in [gname] nil)
        (-> ((gtype graphs) values)
            (vega-tools/validate-and-parse)
            (p/catch #(swap! app-state assoc-in [gname :error] %))
            (p/then #(swap! app-state assoc-in [gname :chart] %)))
        ))

(defn render-chart [[gname {:keys [error chart]}]]
   [:div.col-md-6
     [:h3 (str gname ":") ]
     (cond
       error [:div [:h2 "Validation error"] [:pre (with-out-str (pprint/pprint error))]]
       chart ^{:id gname} [vega-chart {:chart chart}])])

(defn rows [graphs]
  (case (count graphs)
     0 []
     1 [(render-chart (first graphs))]
     (mapv (fn [p] (into [:div.row] p)) (partition 2 (mapv render-chart graphs)))))

(defn main []
  [:div
     [:h1 "Dashboard:"]
     (into [:div.container-fluid] (map render-chart @app-state))])

(defn start! []
  (js/console.log "Starting the app")
  (r/render-component [main] (js/document.getElementById "app")))

;; When this namespace is (re)loaded the Reagent app is mounted to DOM
(start!)
