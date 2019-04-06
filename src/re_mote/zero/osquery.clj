(ns re-mote.zero.osquery
  (:require
   [taoensso.timbre :refer  (refer-timbre)]
   [re-mote.zero.pipeline :refer (run-hosts)]
   [re-mote.zero.functions :refer (osquery)])
  (:import [re_mote.repl.base Hosts]))

(defprotocol Query
  (query [this q]))

(extend-type Hosts
  Query
  (query [this q]
    [this (run-hosts this osquery [q])]))

(defn refer-osquery []
  (require '[re-mote.zero.osquery :as osquery :refer (query)]))
