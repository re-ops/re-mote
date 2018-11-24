(ns re-mote.persist.es
  "Persisting results into Elasticsearch"
  (:require
   re-mote.repl.base
   [clj-time.core :as t]
   [re-mote.log :refer (gen-uuid)]
   [qbits.spandex :as s]
   [com.rpl.specter :refer (transform ALL MAP-VALS multi-path)]
   [re-share.es.common :refer (day-index)]
   [rubber.core :refer (create)]
   [taoensso.timbre :refer (refer-timbre)])
  (:import [re_mote.repl.base Hosts]))

(refer-timbre)

(defprotocol Persistence
  (split [this m f])
  (mult [this] [this m])
  (enrich [this m t]))

(defprotocol Elasticsearch
  (persist
    [this m]
    [this m t])
  (query
    [this q]))

(defn stamp [t]
  (fn [m]
    (merge m {:timestamp (.getMillis (t/now)) :type t})))

(defn by-hosts
  "split results by host"
  [{:keys [result] :as m}]
  (mapv (fn [[host r]] (assoc m :host host :result r)) result))

(defn nested
  "split nested"
  [{:keys [result] :as m}]
  (mapv (fn [r] (assoc m :result r)) result))

(extend-type Hosts
  Persistence
  (enrich [this m t]
    (let [success-stamp (transform [:success ALL] (stamp t) m)
          failure-stamp (transform [:failure MAP-VALS ALL] (stamp (str t ".fail")) success-stamp)]
      [this failure-stamp]))

  (split [this {:keys [success] :as m} f]
    (let [splited (into [] (flatten (map f success)))
          hosts (distinct (map :host splited))]
      [this (assoc m :success splited :hosts hosts)]))

  Elasticsearch
  (persist
    ([this {:keys [success failure] :as m} t]
     (doseq [s success]
       (create (day-index :re-mote :result) t s))
     (doseq [fail (flatten (vals failure))]
       (create (day-index :re-mote :result) t fail))
     [this m])
    ([this m]
     (persist this m :result))))

(def ^:const types {:result {:properties {:timestamp {:type "date" :format "epoch_millis"}
                                          :host {:type "keyword"}
                                          :type {:type "keyword"}}}})

(defn refer-es-persist []
  (require '[re-mote.persist.es :as es :refer (persist enrich split by-hosts nested)]))
