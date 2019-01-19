(ns re-mote.publish.riemann
  "Riemann publish metrics"
  (:require
   [re-share.config :as conf]
   [clj-time.core :refer (now)]
   [clj-time.coerce :as c]
   [riemann.client :as r]
   [clojure.set :refer (rename-keys)]
   [mount.core :as mount :refer (defstate)]))

(defstate riemann
  :start (r/tcp-client (conf/get! :riemann))
  :stop (r/close! riemann))

(defn stat-events [{:keys [type stats timestamp] :as m}]
  (map
   (fn [[k v]]
     (merge {:service k :time timestamp :metric v}
            (select-keys m #{:tags :code :type :host}))) (stats type)))

(defmulti into-events
  (fn [{:keys [type]}] (keyword type)))

(defmethod into-events :cpu [m]
  (stat-events m))

(defmethod into-events :default [m] m)

(defn send-event [e]
  (r/send-event riemann e))
