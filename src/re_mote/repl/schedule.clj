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

(ns re-mote.repl.schedule
  "Schedule tasks"
  (:require
     [clj-time.format :as f]
     [clansi.core :refer (style)]
     [clojure.pprint :refer [pprint]]
     [clj-time.periodic :refer  [periodic-seq]]
     [clojure.core.strint :refer (<<)]
     [taoensso.timbre :refer (refer-timbre)]
     [chime :refer [chime-ch]]
     [clj-time.coerce :as c]
     [clj-time.core :as t]
     [clj-time.local :refer [local-now to-local-date-time]]
     [clojure.core.async :as a :refer [<! go-loop close!]])
  (:import [org.joda.time DateTimeConstants DateTimeZone]))

(refer-timbre)

(def chs (atom {}))
(def status (atom {}))

(defn seconds [n]
  (periodic-seq  (local-now) (-> n t/seconds)))

(defn every-day [hour]
  (let [now (local-now) dates (periodic-seq (.. now (withTime hour 0 0 0)) (t/days 1))]
    (if (> (c/to-long (first dates)) (c/to-long now)) dates (rest dates)))) 

(defn on-weekdays [hour]
  (->> (every-day hour) 
    (remove (comp #{DateTimeConstants/SATURDAY DateTimeConstants/SUNDAY} #(.getDayOfWeek %)))))

(defn at-day [day hour]
  (->> (every-day hour) (filter (comp #{day} #(.getDayOfWeek %)))))

(defn create-ch [k period]
  (let [ch (chime-ch period)]
    (swap! chs assoc k ch)
    (swap! status assoc k {:period period})
     ch
    ))

(defn- run [k ch f args]
  (future
    (a/<!!
      (go-loop []
        (when-let [msg (<! ch)]
          (trace "Chiming at:" msg)
          (let [result (apply f args)]
            (swap! status update k 
              (fn [{:keys [period] :as m}] (merge m {:result result :period (rest period)}))))
       (recur))))))

(defn watch
  "run f using provided period"
   [k period f & args]
   (let [ch (create-ch k period)]
     (run k ch f args) ch))

(defn halt!
   ([]
    (doseq [[k ch] @chs] (halt! k)))
   ([k]
     (when-let [ch (get chs k)] (close! ch))
     (swap! chs (fn [curr] (dissoc curr k))) 
     (swap! status (fn [curr] (dissoc curr k)))
     ))

(defn schedule-report []  
  (doseq [[k {:keys [result period]}] @status]
    (when result (println "Last run result of" (name k) result)))
  (println " ")
  (doseq [[k {:keys [result period]}] (sort-by (fn [[k m]] (first (m :period))) @status)]
    (let [date (f/unparse (f/formatter-local "dd/MM/YY HH:mm:ss") (first period))]
      (println (style date :blue) (<< " ~(name k)")))))

