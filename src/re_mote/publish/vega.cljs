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
