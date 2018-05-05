(ns re-mote.zero.facts
  "Stats metadata etc.."
  (:require
   [com.rpl.specter :refer (transform ALL)]
   [taoensso.timbre :refer  (refer-timbre)]
   [re-mote.zero.pipeline :refer (run-hosts)]
   [re-mote.zero.functions :refer (hardware operating-system)])
  (:import [re_mote.repl.base Hosts]))

(refer-timbre)

(defprotocol Facts
  (os-info [this])
  (hardware-info [this]))

(extend-type Hosts
  Facts
  (os-info [this]
    [this (run-hosts this operating-system [])])
  (hardware-info [this]
    [this (run-hosts this hardware [])]))

(defn results-filter [f success fail hs]
  (let [results (apply merge (transform [ALL] (fn [{:keys [host] :as m}] {host m}) success))]
    (filter (fn [h] (f (results h))) hs)))

(defn refer-facts []
  (require '[re-mote.zero.facts :as facts :refer (os-info hardware-info results-filter)]))
