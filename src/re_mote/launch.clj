(ns re-mote.launch
  (:require
   [re-mote.persist.es :as es]
   [re-mote.zero.cycle :as zero]
   [taoensso.timbre :refer (refer-timbre)]
   [re-mote.api.server :as web]
   [re-mote.repl :as repl]
   [re-share.config :as conf]
   [re-share.zero.keys :as k]
   [re-share.schedule :as sc]))

(refer-timbre)

(defn setup []
  (k/create-server-keys ".curve")
  (conf/load :re-mote (fn [_] {}))
  (repl/setup)
  (es/setup))

(defn start [_]
  (conf/load :re-mote (fn [_] {}))
  (es/start)
  (web/start)
  (zero/start))

(defn stop [_]
  (sc/halt!)
  (zero/stop)
  (web/stop)
  (es/stop))

(defn -main [& args])

(comment
  (stop nil)
  (start nil))
