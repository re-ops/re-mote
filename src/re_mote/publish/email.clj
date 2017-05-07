(ns re_mote.publish.email
  "Generating run result html email"
  (:require
    [re-mote.log :refer (get-logs)]
    [hiccup.core :refer [html]]
    [hiccup.page :refer [html5 include-js include-css]])
 )

(defn summarize [s]
  (let [l (.length s)]
    (if (< l 50) s (.substring s (- l 50) l))))

(defn template [{:keys [success failure]}]
  (html
   (html5
    [:head]
    [:body
      [:h3 "Success:"]
      [:ul 
       (for [{:keys [host out]} success] [:li " &#10003;" host]) 
      ]
      [:h3 "Failure:"]
      [:ul 
       (for [[c rs] failure]
         (for [{:keys [host error out]} (get-logs rs)]
           [:li " &#x2717;" " " host " - " (if out (str c ",") "") (or error (summarize out))]))
       ]
       [:p "For more information please check you local log provider."]
     ]
    )))
