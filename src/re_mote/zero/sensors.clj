(ns re-mote.zero.sensors
  "Sensors monitoring using agent"
  (:require
   [clojure.string :refer (split)]
   [re-mote.zero.shell :refer (args)]
   [re-mote.zero.stats :refer (safe-dec)]
   [com.rpl.specter :as s :refer (transform select MAP-VALS ALL multi-path)]
   [re-mote.zero.functions :refer (shell)]
   [re-mote.zero.pipeline :refer (run-hosts)]
   [re-mote.scripts.sensors :refer (temp-script)])
  (:import [re_mote.repl.base Hosts]))

(defn parse-line
  [line]
  (let [[f s] (split line #"\(")
        corentemp #"(.*)\:\s*\+(\d*\.\d*)"
        [dev r] (rest (re-find corentemp  f))
        limitntemp #"(\w*)\s*\=\s*\+(\d*\.\d*)"
        keywordize (fn [[k v]] [(keyword k) v])
        limits (mapv (fn [l] (keywordize (rest (re-find limitntemp l)))) (split s #"\,"))]
    {:device dev :temp r :limits (into {} limits)}))

(defn parse-lines [lines]
  (mapv parse-line (filter (fn [line] (.contains line "Core")) lines)))

(defn assoc-stats [{:keys [host result] :as m}]
  (let [sections (filter (comp empty? (partial filter empty?)) (partition-by empty? (split (result :out) #"\n")))
        ms (apply merge (mapv (fn [[f & rs]] {(keyword f) (parse-lines rs)}) sections))]
    (assoc-in m [:stats :temperatures] ms)))

(defprotocol Sensors
  (temperature [this]))

(def timeout [2 :second])

(defn into-dec [[this readings]]
  [this (transform [:success ALL :stats MAP-VALS MAP-VALS ALL (multi-path [:limits MAP-VALS] :temp)] safe-dec readings)])

(extend-type Hosts
  Sensors
  (temperature [this]
    (let [{:keys [success failure] :as res} (run-hosts this shell (args temp-script) timeout)]
      (into-dec [this (assoc res :success (map assoc-stats success))]))))

(defn refer-zero-sensors []
  (require '[re-mote.zero.sensors :as zsens]))
