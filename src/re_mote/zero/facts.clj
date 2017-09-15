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

(defn run-hosts [hosts f args]
  (let [uuid (call f args hosts)
        results (collect hosts (-> f fn-meta :name keyword) uuid [5 :minute])
        grouped (group-by :code (vals results))]
    {:hosts hosts :success (grouped 0) :failure (dissoc grouped 0)}))

(defprotocol Facts
  (os-info [this]))

(extend-type Hosts
  Facts
    (os-info [this]
      [this (run-hosts this oshi-os [])]))

(defn used [{:keys [usableSpace totalSpace name]}]
  (when (> totalSpace 0)
    [name (int (* (/ usableSpace totalSpace) 100))]))

(defn space-breach
  "space breach per host for all its disks when usage is above percentage"
  [percent {:keys [result host]}]
  (let [fs (map used (get-in result [:fileSystem :fileStores]))
        breaching (filter (fn [[name u]] (> (- 100 u) percent)) (filter second fs))]
    (when-not (empty? breaching)
      (info "found" breaching "disks for host" host))
    (empty? breaching)
    ))

(defn results-filter [f success _ hs]
  (let [results (apply merge (transform [ALL] (fn [{:keys [host] :as m}] {host m}) success))]
     (filter (fn [h] (f (results h))) hs)))

(defn refer-facts []
  (require '[re-mote.zero.facts :as facts :refer (os-info results-filter space-breach)]))
