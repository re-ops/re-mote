(ns re-mote.persist.es
  "Persisting results into Elasticsearch"
  (:require
   re-mote.repl.base
   [formation.core :as form]
   [clj-time.core :as t]
   [re-mote.log :refer (gen-uuid)]
   [qbits.spandex :as s]
   [com.rpl.specter :refer (transform ALL MAP-VALS multi-path)]
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

(def c (atom nil))

(def es (atom (:es (form/config "re-mote" (fn [_] nil)))))

(defn- exists?
  [index]
  (try
    (= (:status (s/request @c {:url [index] :method :head})) 200)
    (catch Exception e
      (info (ex-data e))
      false)))

(defn- create
  [index mappings]
  (= (:status (s/request @c {:url [index] :method :put :body mappings})) 200))

(defn delete
  [index]
  (= (:status (s/request @c {:url [index] :method :delete})) 200))

(def reactor-stopped "Request cannot be executed; I/O reactor status: STOPPED")

(defn put [t m]
  (try
    (when @c
      (let [req {:url [(@es :index) t] :method :post :body m}
            {:keys [status] :as res} (s/request @c req)]
        (when-not (#{200 201} status)
          (throw (ex-info "failed to persist results" res)))))
    (catch java.lang.IllegalStateException e
       ; system going down its ok
      (when-not (= (-> e Throwable->map :cause) reactor-stopped)
        (throw e)))))

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
       (put t s))
     (doseq [fail (flatten (vals failure))]
       (put t fail))
     [this m])
    ([this m]
     (persist this m "result"))))

(defn start []
  (reset! c (s/client {:hosts [(@es :server)] :basic-auth {:user "elastic" :password "changeme"}})))

(defn stop []
  (when @c
    (s/close! @c)
    (reset! c nil)))

(def ^:const types {:result {:properties {:timestamp {:type "date"}
                                          :host {:type "keyword"}
                                          :type {:type "keyword"}}}})

(defn setup []
  (let [index (@es :index)]
    (start)
    (when-not (exists? index)
      (info "Creating index" index)
      (create index {:mappings types}))))

(defn refer-es-persist []
  (require '[re-mote.persist.es :as es :refer (persist enrich)]))
