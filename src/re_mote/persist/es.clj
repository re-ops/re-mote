(ns re-mote.persist.es
  "Persisting results into Elasticsearch"
  (:require
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


(def c (s/client  {:hosts  ["http://127.0.0.1:9208"]}))

(extend-type Hosts
  Elasticsearch
   (persist [this m]
     ))
