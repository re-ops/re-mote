(ns re-mote.persist.es
  "Persisting results into Elasticsearch"
  (:require
    [re-mote.log :refer (gen-uuid)]
    [qbits.spandex :as s]
    [taoensso.timbre :refer (refer-timbre)])
  (:import [re_mote.repl.base Hosts]))

(refer-timbre)


(defprotocol Elasticsearch
  (persist
    [this m])
  (query
    [this q])
  )


(def server "http://127.0.0.1:9208" )
(def index "re-mote")
(def type- "results")

(def c (s/client  {:hosts [server]}))

(extend-type Hosts
  Elasticsearch
   (persist [this m]
     (s/request c {:url [index type- (gen-uuid)] :method :post :body m})
     [this m]
     ))

(defn refer-es-persist []
  (require '[re-mote.persist.es :as es :refer (persist)]))
