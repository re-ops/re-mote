(ns re-mote.launch
  (:require
    [re-mote.zero.core :refer (start-zero-server stop-zero-server)]
    [taoensso.timbre :refer (refer-timbre)]
    [re-mote.publish.server :as server]
    [re-mote.repl :as repl]
    [re-share.zero.keys :as k]
    [re-mote.repl.schedule :as sc]
    [cliopatra.command :as command :refer  [defcommand]])
  (:gen-class true))

(refer-timbre)

(defn setup []
  (k/create-server-keys ".curve")
  (repl/setup))

(defn start [_]
  (server/start)
  (start-zero-server))

(defn stop [_]
  (sc/halt!)
  (stop-zero-server)
  (server/stop))

(defn -main [& args])

