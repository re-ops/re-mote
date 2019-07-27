(ns re-mote.publish.riemann
  "Riemann publish metrics"
  (:require
   [taoensso.timbre :refer  (refer-timbre)]
   [clojure.core.strint :refer  (<<)]
   [re-share.config.core :as conf]
   [clj-time.core :refer (now)]
   [clj-time.coerce :as c]
   [riemann.client :as r]
   [clojure.set :refer (rename-keys)]
   [com.rpl.specter :refer (transform ALL MAP-VALS select multi-path filterer)]
   [mount.core :as mount :refer (defstate)]))

(refer-timbre)

(defstate riemann
  :start (r/tcp-client (conf/get! :riemann))
  :stop (r/close! riemann))

(defn stat-events [{:keys [type stats timestamp] :as m}]
  (map
   (fn [[k v]]
     (merge {:ttl 60 :service (<< "~{type}/~(name k)") :time timestamp :metric v}
            (select-keys m #{:tags :code :host}))) (stats (keyword type))))

(defmulti into-events
  (fn [{:keys [type]}] (keyword type)))

(defmethod into-events :cpu cpu-events [m]
  (stat-events m))

(defmethod into-events :load load-events [{:keys [type stats timestamp] :as m}]
  (let [cores (select-keys (stats (keyword type)) #{:cores})]
    (map
     (fn [e] (merge e cores)) (stat-events m))))

(defmethod into-events :sensor tmp-events [{:keys [type stats timestamp] :as m}]
  (let [common (select-keys m #{:code :host})]
    (mapcat
     (fn [[k vs]]
       (map
        (fn [{:keys [input device] :as v}]
          (merge {:ttl 60 :service (<< "sensor/~{device}") :time timestamp :metric input} v common)) vs)) (stats (keyword type)))))

(defmethod into-events :free free-events [m]
  (stat-events m))

(defmethod into-events :entropy entropy-events [m]
  (stat-events m))

(defmethod into-events :net network-events [m]
  (stat-events m))

(defn fs? [{:keys [type]}]
  (#{:ext4 :zfs :ext2 :ext3} (keyword type)))

(defn pick-disks [m]
  (first (select [:stats :du (filterer fs?)] m)))

(defmethod into-events :du disk-usage-events [{:keys [timestamp] :as m}]
  (map
   (fn [{:keys [mount type perc]}]
     (merge
      {:ttl 60 :service "disk-usage" :mount mount :time timestamp
       :type type :metric (Integer/valueOf (.replace perc "%" ""))}
      (select-keys m #{:tags :code :host}))) (pick-disks m)))

(defmethod into-events :default [m] [m])

(defn send-event [e]
  (try
    (-> riemann (r/send-event e) (deref 10000 ::timeout))
    (catch java.io.IOException e
      (error "publish to riemann failed"))))
