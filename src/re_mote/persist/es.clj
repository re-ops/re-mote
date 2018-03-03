(ns re-mote.persist.es
  "Persisting results into Elasticsearch"
  (:require
   re-mote.repl.base
   [formation.core :as form]
   [clj-time.core :as t]
   [re-mote.log :refer (gen-uuid)]
   [qbits.spandex :as s]
   [com.rpl.specter :refer (transform ALL MAP-VALS multi-path)]
   [re-share.es.node :as node]
   [re-share.es.common :refer (create create-index exists?)]
   [taoensso.timbre :refer (refer-timbre)])
  (:import [re_mote.repl.base Hosts]))

(refer-timbre)

(defprotocol Persistence
  (enrich [this m t]))

(defprotocol Elasticsearch
  (persist
    [this m]
    [this m t])
  (query
    [this q]))

(def es (atom (:es (form/config "re-mote" (fn [_] nil)))))

(defn index []
  (@es :index))

(defn stamp [t]
  (fn [m]
    (merge m {:timestamp (.getMillis (t/now)) :type t})))

(extend-type Hosts
  Persistence
  (enrich [this m t]
    (let [success-stamp (transform [:success ALL] (stamp t) m)
          failure-stamp (transform [:failure MAP-VALS ALL] (stamp (str t ".fail")) success-stamp)]
      [this failure-stamp]))
  Elasticsearch
  (persist
    ([this {:keys [success failure] :as m} t]
     (doseq [s success]
       (create (index) t m))
     (doseq [fail (flatten (vals failure))]
       (create (index) t m))
     [this m])
    ([this m]
     (persist this m "result"))))

(defn start []
  (node/connect @es))

(defn stop []
  (node/stop))

(def ^:const types {:result {:properties {:timestamp {:type "date"}
                                          :host {:type "keyword"}
                                          :type {:type "keyword"}}}})

(defn setup []
  (start)
  (when-not (exists? (index))
    (info "Creating index" (index))
    (create-index (index) {:mappings types})))

(defn refer-es-persist []
  (require '[re-mote.persist.es :as es :refer (persist enrich)]))
