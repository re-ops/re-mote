(ns re-mote.zero.facts
  "Stats metadata etc.."
  (:require
   [com.rpl.specter :refer (transform ALL)]
   [clojure.tools.trace :as t]
   [re-mote.zero.functions :as fns :refer (fn-meta)]
   [taoensso.timbre :refer  (refer-timbre)]
   [re-mote.zero.base :refer (refer-zero-base)]
   [re-mote.zero.functions :refer (refer-zero-fns)])
  (:import [re_mote.repl.base Hosts]))

(refer-timbre)
(refer-zero-base)
(refer-zero-fns)

(defn run-hosts
  ([hosts f args]
   (run-hosts hosts f args [10 :second]))
  ([hosts f args timeout]
   (let [uuid (call f args hosts)
         results (collect hosts (-> f fn-meta :name keyword) uuid timeout)
         grouped (group-by :code (vals results))]
     {:hosts hosts :success (grouped 0) :failure (dissoc grouped 0)})))

(defprotocol Facts
  (os-info [this])
  (hardware-info [this]))

(extend-type Hosts
  Facts
  (os-info [this]
    [this (run-hosts this oshi-os [])])
  (hardware-info [this]
    [this (run-hosts this oshi-hardware [])]))

(defn results-filter [f success fail hs]
  (println fail)
  (let [results (apply merge (transform [ALL] (fn [{:keys [host] :as m}] {host m}) success))]
    (filter (fn [h] (f (results h))) hs)))

(defn refer-facts []
  (require '[re-mote.zero.facts :as facts :refer (os-info hardware-info results-filter space-breach)]))
