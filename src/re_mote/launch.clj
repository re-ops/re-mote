(ns re-mote.launch
  (:require
   [re-mote.zero.core :as zero]
   [taoensso.timbre :refer (refer-timbre)]
   [re-mote.publish.server :as web]
   [re-mote.repl :as repl]
   [re-share.zero.keys :as k]
   [re-mote.repl.schedule :as sc])
  (:gen-class true))

(refer-timbre)

(defn setup []
  (k/create-server-keys ".curve")
  (repl/setup))

(defn start [_]
  (web/start)
  (zero/start))

(defn stop [_]
  (sc/halt!)
  (zero/stop)
  (web/stop))

(defn -main [& args])

(comment
  (stop nil)
  (start nil))
