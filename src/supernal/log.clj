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

(ns supernal.log
  "log collection"
  (:require
      [taoensso.timbre :refer (refer-timbre)]
      [clojure.core.strint :refer (<<)]
      [chime :refer [chime-ch]]
      [clj-time.periodic :refer  [periodic-seq]]
      [clj-time.core :as t]
      [clj-time.coerce :refer [to-long]]
      [clojure.java.io :refer (reader)]
      [clojure.core.async :as a :refer [<! go-loop close!]]))

(refer-timbre)

(def logs (atom {}))

(defn log-output
  "Output log stream"
  [out host]
  (doseq [line (line-seq (reader out))] (debug  (<< "[~{host}]:") line)))

(defn collect-log
  "Collect log output into logs atom"
  [uuid]
   (fn [out host]
     (let [lines (doall (map (fn [line] (info  (<< "[~{host}]:") line) line) (line-seq (reader out))))]
       (swap! logs (fn [m] (assoc m uuid  {:ts (t/now) :lines lines}))))))

(defn get-log
  "Getting log entry and clearing it"
  [uuid]
   (when-let [{:keys [lines]} (get @logs uuid)]
      (swap! logs (fn [m] (dissoc m uuid)))
      lines
     ))

(defn purge
  "Clearing dead non collected logs"
  []
  (let [minut-ago (to-long (t/minus (t/now) (t/minutes 1)))
        old (filter (fn [[uuid {:keys [ts]}]] (<= (to-long ts) minut-ago)) @logs)]
     (doseq [[uuid _] old]
       (trace "purged log" uuid)
       (swap! logs (fn [m] (dissoc m uuid))))))

(defn create-ch [s]
  (chime-ch (periodic-seq (t/now) (-> s t/seconds))))

(defn run-purge [s]
  (let [ch (create-ch s)]
    (future
      (a/<!!
        (go-loop []
          (when-let [msg (<! ch)]
            (debug "purging logs at" (t/now))
            (purge)
        (recur)))))))

(defn gen-uuid [] (.replace (str (java.util.UUID/randomUUID)) "-" ""))
