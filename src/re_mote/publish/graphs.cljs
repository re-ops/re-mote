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

(ns supernal.publish.graphs
  "common vega graphs")

(defn lines [vs]
  {:padding {:top 10, :left 30, :bottom 30, :right 10}

   :data [{:name "table" :values vs}]

   :scales [
    {:name "x"
     :type "ordinal"
     :range "width"
     :domain {:data "table", :field "x"}}
    {:name "y"
     :type "linear"
     :range "height"
     :domain {:data "table", :field "y"}, :nice true}]

   :axes [{:type "x", :scale "x"} {:type "y", :scale "y"}]

   :marks [
    {:type "rect", :from {:data "table"},
     :properties {
        :enter {
           :x {:scale "x", :field "x"}
           :width {:scale "x", :band true, :offset -1}
           :y {:scale "y", :field "y"}
           :y2 {:scale "y", :value 0}
         }
         :update {:fill {:value "steelblue"}}
         :hover {:fill {:value "red"}}}}]
   })

(defn stack
  "See https://vega.github.io/vega/examples/stacked-area-chart/"  
  [vs]
  {:padding {:top 10, :left 30, :bottom 30, :right 10},
   :data
   [{:name "table", :values vs}
    {:name "stats",
     :source "table",
     :transform
     [{:type "aggregate",
	 :groupby ["x"],
	 :summarize [{:field "y", :ops ["sum"]}]}]}],
   :scales
   [{:name "x",
     :type "ordinal",
     :range "width",
     :points true,
     :domain {:data "table", :field "x"}}
    {:name "y",
     :type "linear",
     :range "height",
     :nice true,
     :domain {:data "stats", :field "sum_y"}}
    {:name "color",
     :type "ordinal",
     :range "category10",
     :domain {:data "table", :field "c"}}],
   :axes [{:type "x", :scale "x"} {:type "y", :scale "y"}],
   :marks
   [{:type "group",
     :from
     {:data "table",
	:transform
	[{:type "stack", :groupby ["x"], :sortby ["c"], :field "y"}
	 {:type "facet", :groupby ["c"]}]},
     :marks
     [{:type "area",
	 :properties
	 {:enter
	  {:interpolate {:value "monotone"},
	   :x {:scale "x", :field "x"},
	   :y {:scale "y", :field "layout_start"},
	   :y2 {:scale "y", :field "layout_end"},
	   :fill {:scale "color", :field "c"}},
	  :update {:fillOpacity {:value 1}},
	  :hover {:fillOpacity {:value 0.5}}}}]}]}
  )

(defn stock [vs] 
  {:data [{:name "table", :values vs}]
   :scales [ {
     :name "x",
     :type "time",
     :range "width",
     :domain {:data "table", :field "x"}}
    {:name "y",
     :type "linear",
     :range "height",
     :nice true,
     :domain {:data "table", :field "y"}}
    {:name "color",
     :type "ordinal",
     :domain {:data "table", :field "host"},
     :range "category10"}],
   :axes [{
     :type "x", :scale "x", :tickSizeEnd 0} {:type "y", :scale "y"}],
   :marks [{
     :type "group",
     :from
     {:data "table", :transform [{:type "facet", :groupby ["host"]}]},
     :marks
     [{:type "line",
       :properties
       {:enter
        {:x {:scale "x", :field "x"},
         :y {:scale "y", :field "y"},
         :stroke {:scale "color", :field "host"},
         :strokeWidth {:value 2}}}}
      {:type "text",
       :from
       {:transform
        [{:type "filter", :test "datum.date == 1267430400000"}]},
       :properties
       {:enter
        {:x {:scale "x", :field "date", :offset 2},
         :y {:scale "y", :field "value"},
         :fill {:scale "color", :field "host"},
         :text {:field "host"},
         :baseline {:value "middle"}}}}]}]})

(def defaults {:width 300 :height 200})

(defn with-defaults [[k f]] 
  [k (fn [vs] (merge (f vs) defaults))])
 
(def graphs (into {} (map with-defaults {:vega/stack stack :vega/lines lines :vega/stock stock})))
