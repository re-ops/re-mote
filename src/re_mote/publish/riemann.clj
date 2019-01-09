(ns re-mote.publish.riemann
  "Riemann publish metrics"
  (:require
   [clj-time.core :refer (now)]
   [clj-time.coerce :as c]
   [riemann.client :as r]
   [mount.core :as mount :refer (defstate)]))

(defstate riemann
  :start (r/tcp-client (config :riemann))
  :stop (r/close! riemann))

(defn send-event [m]
  (r/send-event riemann (assoc m :time (c/to-epoch (now)))))
