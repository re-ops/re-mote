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

(ns re-mote.log
  "log collection"
  (:require
      [clojure.string :refer (join)]
      [taoensso.timbre.appenders.3rd-party.rolling :refer (rolling-appender)]
      [taoensso.timbre.appenders.core :refer (println-appender)]
      [clansi.core :refer (style)]
      [taoensso.timbre :refer (refer-timbre set-level! merge-config!)]
      [clojure.core.strint :refer (<<)]
      [chime :refer [chime-ch]]
      [clj-time.core :as t]
      [clj-time.coerce :refer [to-long]]
      [clojure.java.io :refer (reader)]
      [re-mote.repl.schedule :refer (watch seconds)]))

(refer-timbre)

(def logs (atom {}))

(defn log-output
  "Output log stream"
  [out host]
  (doseq [line (line-seq (reader out))]
    (debug  (<< "[~{host}]:") line)))

(defn process-line
   "process a single log line"
   [host line]
  (info (<< "[~{host}]:") line) line)

(defn collect-log
  "Collect log output into logs atom"
  [uuid]
   (fn [out host]
     (let [lines (doall (map (partial process-line host) (line-seq (reader out))))]
       (swap! logs (fn [m] (assoc m uuid  {:ts (t/now) :lines lines}))))))

(defn get-log
  "Getting log entry and clearing it"
  [uuid]
   (when-let [{:keys [lines]} (get @logs uuid)]
      (swap! logs (fn [m] (dissoc m uuid)))
       lines
     ))

(defn get-logs
  "Getting logs for all hosts"
  [hosts]
  (doall
    (map
      (fn [{:keys [uuid] :as m}]
        (if-not uuid m
          (dissoc (assoc m :out (join "\n" (get-log uuid))) :uuid))) hosts)))

(defn purge
  "Clearing dead non collected logs"
  []
  (let [minut-ago (to-long (t/minus (t/now) (t/minutes 1)))
        old (filter (fn [[uuid {:keys [ts]}]] (<= (to-long ts) minut-ago)) @logs)]
     (doseq [[uuid _] old]
       (trace "purged log" uuid)
       (swap! logs (fn [m] (dissoc m uuid))))))

(defn run-purge [s]
  (watch :logs-purge (seconds s) (fn [] (debug "purging logs at" (t/now)) (purge))))

(defn gen-uuid [] (.replace (str (java.util.UUID/randomUUID)) "-" ""))

(defn output-fn
  "Timbre logger format function"
  ([data] (output-fn nil data))
  ([opts data] ; For partials
   (let [{:keys [level ?err #_vargs msg_ ?ns-str ?file hostname_ timestamp_ ?line]} data]
     (str (style "> " :blue) (force msg_)))))

(defn disable-coloring
   "See https://github.com/ptaoussanis/timbre"
   []
  (merge-config!
    {:output-fn (partial output-fn  {:stacktrace-fonts {}})})
  (merge-config!  {
     :appenders {
       :println  (merge {:ns-whitelist ["re-mote.output"]}
                        (println-appender {:stream :auto }))
       :rolling (rolling-appender {:path "re-mote.log" :pattern :weekly})}}))

(defn setup-logging
  "Sets up logging configuration:
    - stale logs removale interval
    - steam collect logs
    - log level
  "
  [& {:keys [interval level] :or {interval 10 level :info}}]
  (disable-coloring)
  (set-level! level)
  (run-purge interval))

