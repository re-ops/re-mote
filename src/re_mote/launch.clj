(ns re-mote.launch
  (:require
   [re-mote.persist.es :refer (types)]
   [re-mote.zero.cycle :as zero]
   [taoensso.timbre :refer (refer-timbre)]
   [re-mote.api.server :as web]
   [re-share.components.core :refer (start-all stop-all setup-all)]
   [re-share.components.elastic :as es]
   [re-mote.repl :as repl]
   [re-share.config :as conf]
   [re-share.zero.keys :as k]
   [re-share.schedule :as sc]))

(refer-timbre)

(defn build-components [] {:es (es/instance types :re-mote true) :zero (zero/instance) :web (web/instance)})

(defn setup []
  (let [components (build-components)]
    (k/create-server-keys ".curve")
    (conf/load (fn [_] {}))
    (repl/setup)
    (setup-all components)
    components))

(defn start [components]
  (conf/load (fn [_] {}))
  (start-all components)
  components)

(defn stop [components]
  (sc/halt!)
  (stop-all components)
  components)

(defn -main [& args])

(comment
  (stop nil)
  (start nil))
