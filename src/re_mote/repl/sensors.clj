(comment
  re-mote, Copyright 2017 Ronen Narkis, narkisr.com
  Licensed under the Apache License,
  Version 2.0  (the "License") you may not use this file except in compliance with the License.
  You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.)

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
