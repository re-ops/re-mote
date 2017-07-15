(ns re-mote.repl.sensors
  "Sensors monitoring"
  (:require
     [clojure.string :refer (split)]
     [re-mote.log :refer (get-log get-logs)]
     [re-mote.repl.base :refer (run-hosts)]
     [pallet.stevedore :refer (script)])
  (:import [re_mote.repl.base Hosts]))

(defn sensors-script []
  (script
    (pipe ("sensors -A") ("awk" "'{$1=$1};1'"))))

(defn parse-lines [lines]
  (mapv
    (fn [line]
      (let [[f s] (split line #"\(" )
            corentemp #"(.*)\:\s*\+(\d*\.\d*)"
            [dev r] (rest (re-find corentemp  f))
            limitntemp #"(\w*)\s*\=\s*\+(\d*\.\d*)"
            keywordize (fn [[k v]] [(keyword k) v])
            limits (mapv (fn [l] (keywordize (rest (re-find limitntemp l)))) (split s #"\,"))]
         {:device dev :temp r :limits (into {} limits)})) lines))

(defn assoc-stats [{:keys [host out] :as m}]
  (let [sections (filter (comp empty? (partial filter empty?)) (partition-by empty? (split out #"\n")))
        ms (apply merge (mapv (fn [[f & rs]] {(keyword f) (parse-lines rs)}) sections))]
     (assoc-in m [:stats :temperatures] ms)))

(defprotocol Sensors
  (temperature [this]))

(extend-type Hosts
  Sensors
  (temperature [this]
   (let [{:keys [success failure] :as res} (run-hosts this (sensors-script))]
     [this (assoc res :success (map assoc-stats (get-logs success)))])))

(defn refer-sensors []
  (require '[re-mote.repl.sensors :as sensors :refer (temperature)]))
