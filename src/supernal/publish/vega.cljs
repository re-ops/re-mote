(ns supernal.publish.vega
  (:require 
    [supernal.publish.graphs :refer (graphs)]
    [cljs.reader :as reader]
    [clojure.pprint :as pprint]
    [promesa.core :as p]
    [reagent.core :as r]
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
    (swap! app-state assoc :chart nil :error nil)
    (-> ((graph graphs) values)
        (vega-tools/validate-and-parse)
        (p/catch #(swap! app-state assoc :error %))
        (p/then #(swap! app-state assoc :chart %))))

(defn main []
  (let [{:keys [input error chart]} @app-state]
    [:div
     [:h1 "Dashboard:"]
     [:div.container-fluid
      [:div.col-md-6
       (cond
         error [:div
                [:h2 "Validation error"]
                [:pre (with-out-str (pprint/pprint error))]]
         chart [vega-chart {:chart chart}]
         )]]]))

(defn start! []
  (js/console.log "Starting the app")
  (r/render-component [main] (js/document.getElementById "app")))

;; When this namespace is (re)loaded the Reagent app is mounted to DOM
(start!)
