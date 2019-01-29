(ns re-mote.publish.riemann
  "Riemann publish metrics"
  (:require
   [clojure.core.strint :refer  (<<)]
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
     (merge {:service (<< "~{type}/~(name k)") :time timestamp :metric v}
            (select-keys m #{:tags :code :host}))) (stats (keyword type))))

(defmulti into-events
  (fn [{:keys [type]}] (keyword type)))

(defmethod into-events :cpu cpu-events [m]
  (stat-events m))

(defmethod into-events :load load-events [m]
  (stat-events m))

(defmethod into-events :temperature tmp-events [m]
  (stat-events m))

(defmethod into-events :free free-events [m]
  (stat-events m))

(defmethod into-events :net network-events [m]
  (stat-events m))

(defmethod into-events :default [m] [m])

(defn send-event [e]
  (-> riemann (r/send-event e) (deref 10000 ::timeout)))
