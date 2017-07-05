(ns re-mote.zero.core
  (:require
    [taoensso.timbre :refer  (refer-timbre)]
    [re-mote.zero.common :refer  (context)]
    [re-mote.zero.server :refer (setup-server kill-server! bind)]
    [re-mote.zero.worker :refer (setup-workers stop-workers!)]))

(refer-timbre)

(defn start-zero-server []
  (let [ctx (context)]
    (setup-server ctx ".curve/server-private.key")
    (setup-workers ctx 4)
    (future (bind))
    ))

(defn stop-zero-server []
  (stop-workers!)
  (kill-server!))


(comment
  (setup)
  (stop)
  )
