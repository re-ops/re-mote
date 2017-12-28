(ns re-mote.persist.es
  "Persisting results into Elasticsearch"
  (:require
   re-mote.repl.base
   [formation.core :as form]
   [re-mote.log :refer (gen-uuid)]
   [qbits.spandex :as s]
   [taoensso.timbre :refer (refer-timbre)])
  (:import [re_mote.repl.base Hosts]))

(refer-timbre)

(defprotocol Elasticsearch
  (persist
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

(extend-type Hosts
  Elasticsearch
  (persist [this m t]
    (let [req {:url [(@es :index) t (gen-uuid)] :method :post :body m}
          {:keys [status] :as res} (s/request @c req)]
      (if (#{200 201} status)
        [this m]
        (throw (ex-info "failed to persist results" res))))))

(defn start []
  (reset! c 
    (s/client {:hosts [(@es :server)] :basic-auth {:user "elastic" :password "changeme"}})))

(defn stop []
  (when @c
    (s/close! @c)
    (reset! c nil)))

(def ^:const types
  {:stats { :properties { :success {:date {:type "date"}}}}})

(defn setup []
  (let [index (@es :index)]
    (start)
    (when-not (exists? index)
      (info "Creating index" index)
      (create index {:mappings types}))))

(defn refer-es-persist []
  (require '[re-mote.persist.es :as es :refer (persist)]))

(comment
  (start)
  (try
    (s/request @c
               {:url [(es :index) (es :type) (gen-uuid)]
                :method :post :body {:tag "hello!" :uuid (gen-uuid)}})
    (catch Exception e
      (println e))))
