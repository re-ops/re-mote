(ns re-mote.zero.facts
  "Stats metadata etc.."
  (:require
   [com.rpl.specter :refer (transform ALL)]
   [clojure.tools.trace :as t]
   [re-mote.zero.functions :as fns :refer (fn-meta)]
   [taoensso.timbre :refer  (refer-timbre)]
   [re-mote.zero.base :refer (run-hosts)]
   [re-mote.zero.functions :refer (refer-zero-fns)])
  (:import [re_mote.repl.base Hosts]))

(refer-timbre)
(refer-zero-fns)

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
  (let [results (apply merge (transform [ALL] (fn [{:keys [host] :as m}] {host m}) success))]
    (filter (fn [h] (f (results h))) hs)))

(defn refer-facts []
  (require '[re-mote.zero.facts :as facts :refer (os-info hardware-info results-filter)]))
