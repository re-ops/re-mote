(ns re-mote.launch
  (:require
   [re-mote.persist.es :as es]
   [re-mote.zero.cycle :as zero]
   [taoensso.timbre :refer (refer-timbre)]
   [re-mote.publish.server :as web]
   [re-mote.repl :as repl]
   [re-share.zero.keys :as k]
   [re-mote.repl.schedule :as sched])
  (:gen-class true))

(refer-timbre)

(defn setup []
  (k/create-server-keys ".curve")
  (es/setup)
  (repl/setup))

(defn start [_]
  (es/start)
  (web/start)
  (zero/start))

(defn stop [_]
  (sched/halt!)
  (zero/stop)
  (es/stop)
  (web/stop))

(defn -main [& args])

(comment
  (stop nil)
  (start nil))
