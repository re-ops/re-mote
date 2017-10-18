(ns re-mote.persist.es
  "Persisting results into Elasticsearch"
  (:require
    [formation.core :as form]
    [re-mote.log :refer (gen-uuid)]
    [qbits.spandex :as s]
    [taoensso.timbre :refer (refer-timbre)])
  (:import [re_mote.repl.base Hosts]))

(refer-timbre)

(defprotocol Elasticsearch
  (persist
    [this m])
  (query
    [this q]))

(def c (atom nil))

(def es
  (memoize (fn [k] ((:es (form/config "re-mote" (fn [_] nil))) k))))

(extend-type Hosts
  Elasticsearch
   (persist [this m]
     (let [req {:url [(es :index) (es :type) (gen-uuid)] :method :post :body m}
           {:keys [status] :as res} (s/request @c req)]
       (if (#{200 201} status)
        [this m]
        (throw (ex-info "failed to persist results" res) )
         ))))

(defn start []
  (reset! c (s/client {:hosts [(es :server)] :basic-auth {:user "elastic" :password "changeme"}})))

(defn stop []
  (reset! c nil))
 
(defn refer-es-persist []
  (require '[re-mote.persist.es :as es :refer (persist)]))


(comment
  (start) 
  (s/request @c 
    {:url [(es :index) (es :type) (gen-uuid)] 
     :method :post :body {:results "hello!"}}))
