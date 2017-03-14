(ns supernal.publish.graphs
  "common vega graphs"
 )

(defn lines [vs]
  {:width  600
   :height 100
   :padding {:top 10, :left 30, :bottom 30, :right 10}

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
  {:width 500,
   :height 200,
   :padding {:top 10, :left 30, :bottom 30, :right 10},
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

(def graphs {:vega/stack stack :vega/lines lines})
