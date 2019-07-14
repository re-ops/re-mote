(ns re-mote.zero.sensors
  "Sensors monitoring using agent"
  (:require
   [clojure.spec.alpha :as s]
   [clojure.string :refer (split join trim replace-first)]
   [re-cog.scripts.common :refer (shell-args)]
   [re-mote.zero.stats :refer (safe-dec)]
   [com.rpl.specter :refer (transform select MAP-VALS ALL multi-path)]
   [re-cog.resources.exec :refer (shell)]
   [re-mote.zero.pipeline :refer (run-hosts)]
   [re-cog.scripts.sensors :refer (temp-script)]
   re-mote.repl.base)
  (:import [re_mote.repl.base Hosts]))

(defn into-map
  "Convert key value pairs into a map"
  [ps]
  (into {}
        (map
         (fn [p]
           (let [[k v] (split p #"\:\s")]
             [(keyword (replace-first (trim k) #".*\_" "")) (safe-dec v)])) ps)))

(defn process-section [[parent & metrics]]
  (letfn [(into-key [s] (join "" (drop-last s)))]
    (let [sub-sections (partition 2 (partition-by (fn [line] (re-matches #".*\:$" line)) metrics))]
      {(keyword parent)
       (mapv
        (fn [[[k] pairs]]
          (let [device (into-key k)]
            (merge {:device device} (into-map pairs)))) sub-sections)})))

(defn assoc-stats [{:keys [host result] :as m}]
  (let [lines (split (result :out) #"\n")
        sections (filter (comp empty? (partial filter empty?)) (partition-by empty? lines))]
    (assoc-in m [:stats :temperature] (into {} (map process-section sections)))))

(defprotocol Sensors
  (temperature [this]))

(def timeout [2 :second])

(extend-type Hosts
  Sensors
  (temperature [this]
    (let [{:keys [success failure] :as res} (run-hosts this shell (shell-args temp-script) timeout)]
      [this (assoc res :success (map assoc-stats success))])))

(defn refer-zero-sensors []
  (require '[re-mote.zero.sensors :as zsens]))

