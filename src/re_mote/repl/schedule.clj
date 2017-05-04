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
      [clj-time.periodic :refer  [periodic-seq]]
      [taoensso.timbre :refer (refer-timbre)]
      [chime :refer [chime-ch]]
      [clj-time.core :as t]
      [clj-time.local :refer [local-now]]
      [clojure.core.async :as a :refer [<! go-loop close!]])
  (:import [org.joda.time DateTimeConstants DateTimeZone]))

(refer-timbre)

(def chs (atom {}))

(defn seconds [n]
  (periodic-seq  (t/now) (-> n t/seconds)))

(defn every-day [hour]
  (periodic-seq (.. (local-now) (withTime hour 0 0 0)) (t/days 1))) 

(defn on-weekdays [hour]
  (->> (every-day hour) 
    (remove (comp #{DateTimeConstants/SATURDAY DateTimeConstants/SUNDAY} #(.getDayOfWeek %)))))

(defn at-day [hour day]
  (->> (every-day hour) (filter (comp #{day} #(.getDayOfWeek %)))))

(defn create-ch [k period]
  (let [ch (chime-ch period)]
    (swap! chs assoc k ch)
     ch
    ))

(defn- run [ch f args]
  (future
    (a/<!!
      (go-loop []
        (when-let [msg (<! ch)]
          (trace "Chiming at:" msg)
          (apply f args)
       (recur))))))

(defn watch
  "run f using provided period"
   [k period f & args]
   (let [ch (create-ch k period)]
     (run ch f args) ch))

(defn halt!
   ([]
    (doseq [[k ch] @chs] (halt! k)))
   ([k]
     (close! (get chs k))
     (swap! chs (fn [curr] (dissoc curr)))))

(defn pprint-scheds [] 
  (clojure.pprint/pprint @chs)
  )
