(comment
  Celestial, Copyright 2017 Ronen Narkis, narkisr.com
  Licensed under the Apache License,
  Version 2.0  (the "License") you may not use this file except in compliance with the License.
  You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.)

(ns supernal.repl.stats
  "General stats"
  (:require
    [taoensso.timbre :refer (refer-timbre)]
    [com.rpl.specter :as s :refer (transform select MAP-VALS ALL ATOM keypath srange)]
    [clj-time.core :as t]
    [clj-time.coerce :refer (to-long)]
    [supernal.repl.base :refer (run-hosts zip)]
    [supernal.repl.schedule :refer (watch seconds)]
    [pallet.stevedore :refer (script do-script)])
  (:import [supernal.repl.base Hosts]))

(refer-timbre)

(defprotocol Stats
  (cpu [this] [this m])
  (free [this] [this m])
  (collect [this m])
  (sliding [this m f k]))

(defn bash!
   "check that we are running within bash!"
   []
  (script
     ("[" "!" "-n" "\"$BASH\"" "]" "&&" "echo" "Please set default user shell to bash" "&&" "exit" 1)))

(defn cpu-script []
   (script
     (set! R @("mpstat" "1" "1"))
     (if (not (= $? 0)) ("exit" 1))
     (pipe ((println (quoted "${R}"))) ("awk" "'NR==4 { print $4 \" \" $6 \" \" $13 }'"))))

(defn free-script []
   (script
     (set! R @("free" "-m"))
     (if (not (= $? 0)) ("exit" 1))
     (pipe ((println (quoted "${R}"))) ("awk" "'NR==2 { print $2 \" \" $3 \" \" $4 }'"))))

(defn validate! [f]
  (do-script (bash!) (f)))

(def readings (atom {}))

(defn into-dec [[this readings]]
  [this (transform [:success ALL :stats MAP-VALS MAP-VALS] bigdec readings)])

(defn avg 
  "Windowed average function"
  [ts]
  (let [sum (reduce (fn [a [t m]] (merge-with + a m)) {} ts)]
    {(-> ts first first) (transform [MAP-VALS] (fn [n] (with-precision 10 (/ n (count ts)))) sum)}))

(defn- window [f ts]
  (apply merge (map f (partition 3 1 ts))))

(defn reset 
  "reset a key in readings"
  [k]
  (transform [ATOM MAP-VALS MAP-VALS] (fn [m] (dissoc m k)) readings))

(defn select-
  "select a single key from readings"
  [k]
  (select [ATOM MAP-VALS MAP-VALS (keypath k)] readings))

(defn last-n 
  "keep last n items of a sorted map"
  [n m]
   (let [v (into [] (into (sorted-map) m)) c (count v)]
     (if (< c n) m (into (sorted-map) (subvec v (- c n) c)))))

(extend-type Hosts
  Stats
  (cpu 
    ([this] 
      (into-dec (zip this (run-hosts this (validate! cpu-script)) :stats :cpu :usr :sys :idle)))
     ([this _] 
      (cpu this)))

  (free 
    ([this] 
       (into-dec (zip this (run-hosts this (validate! free-script)) :stats :free :total :used :free)))
     ([this _]
      (free this)))

  (collect [this {:keys [success] :as m}]
    (doseq [{:keys [host stats]} success]
      (doseq [[k v] stats]
        (swap! readings update-in [host k :timeseries] 
          (fn [m] (if (nil? m) (sorted-map (t/now) v) (assoc m (t/now) v))))))
     [this m])

  (sliding [this {:keys [success] :as m} f fk]
    (doseq [{:keys [host stats]} success]
      (doseq [[k _] stats]
        (transform [ATOM (keypath host) k]
           (fn [{:keys [timeseries] :as m}] (assoc m fk (window f timeseries))) readings)))
    [this m] 
    ))

(defn purge [n]
   (transform [ATOM MAP-VALS MAP-VALS MAP-VALS] (partial last-n n) readings))

(defn setup-stats
   "Setup stats collection" 
   [s n]
   (watch :stats-purge (seconds s) (fn [] (purge n))))

(defn- host-values
  [k ks {:keys [host stats]}]
  (transform [ALL] 
    (fn [[t s]] {:x (to-long t) :y (get-in s ks) :host host})
      (into [] (get-in @readings [host (first (keys stats)) k]))))

(defn single-per-host 
  "Collect a single nested reading for each host"
  [k ks success]
   (mapcat (partial host-values k ks) success))

(defn- avg-data-point [& ks] 
  (let [[t _] (first ks) sums (apply (partial merge-with +) (map second ks))
        vs (transform [MAP-VALS] #(with-precision 10 (/ % (count ks))) sums)] 
    (map (fn [[k v]] {:x (to-long t) :y v :c k}) vs)))

(defn avg-all
  "Average for all hosts"
  [k success]
  (let [r (first (keys (:stats (first success))))]
    (apply mapcat avg-data-point (select [ATOM MAP-VALS r k] readings))))

(defn refer-stats []
  (require '[supernal.repl.stats :as stats :refer (cpu free collect sliding avg setup-stats)]))

(comment
 (reset! readings {}))

