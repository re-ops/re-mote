(ns re-mote.repl.sensors
  "Sensors monitoring over ssh"
  (:require
   [re-mote.ssh.pipeline :refer (run-hosts)]
   [re-mote.scripts.sensors :refer (temp-script)]
   [re-mote.zero.sensors :refer (assoc-stats into-dec)]
   re-mote.repl.base)
  (:import
   [re_mote.repl.base Hosts]))

(defprotocol Sensors
  (temperature [this]))

(extend-type Hosts
  Sensors
  (temperature [this]
    (let [{:keys [success failure] :as res} (run-hosts this (temp-script))]
      (into-dec [this (assoc res :success (map assoc-stats success))]))))

(defn refer-sensors []
  (require '[re-mote.repl.sensors :as sens]))
