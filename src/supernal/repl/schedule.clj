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

(ns supernal.repl.schedule
  "Schedule tasks"
  (:require 
      [clj-time.periodic :refer  [periodic-seq]]
      [taoensso.timbre :refer (refer-timbre)]
      [chime :refer [chime-ch]]
      [clj-time.core :as t]
      [clojure.core.async :as a :refer [<! go-loop close!]]))

(refer-timbre)

(def chs (atom #{}))

(defn create-ch [s] 
  (let [ch (chime-ch (periodic-seq  (t/now) (-> s t/seconds)))]
    (swap! chs conj ch) 
     ch
    ))

(defn- run [ch f args]
 (future
   (a/<!! 
     (go-loop []
       (when-let [msg (<! ch)]
         (debug "Chiming at:" msg)
         (apply f args)
      (recur))))))

(defn watch
  "run f every s seconds"
   [s f & args]
   (let [ch (create-ch s)] (run ch f args) ch))

(defn halt!
   ([] 
    (doseq [ch @chs] (halt! ch)))
   ([ch] 
     (close! ch) 
     (swap! chs (fn [curr] (into #{} (remove #{ch} curr))))) 
    )
   
