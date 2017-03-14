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
    [clj-time.core :as t]
    [supernal.repl.base :refer (run-hosts zip)]
    [pallet.stevedore :refer (script do-script)])
  (:import [supernal.repl.base Hosts]))

(defprotocol Stats
  (cpu [this])
  (free [this])
  (collect [this m])
  (sliding [this m]))

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

(def db (atom {}))

(extend-type Hosts
  Stats
  (cpu [this]
    (zip this (run-hosts this (validate! cpu-script)) :stats :cpu :usr :sys :idle))

  (free [this]
    (zip this (run-hosts this (validate! free-script)) :stats :free :total :used :free))

  (collect [this {:keys [success] :as m}]
    (doseq [{:keys [host stats]} success]
      (doseq [[k v] stats]
        (swap! db assoc-in [host k (t/now)] v))))

  (sliding [this {:keys [success] :as m}]
    
    ) 
  )

(defn refer-stats []
  (require '[supernal.repl.stats :as stats :refer (cpu free collect)]))
