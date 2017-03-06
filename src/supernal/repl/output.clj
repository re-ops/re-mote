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

(ns supernal.repl.output
  (:require
    [taoensso.timbre.appenders.3rd-party.rolling :refer (rolling-appender)] 
    [clojure.pprint :refer [print-table]]
    [clansi.core :refer (style)]
    [clojure.string :as s]
    [taoensso.timbre :refer (refer-timbre set-level! merge-config!)])
  (:import [supernal.repl.base Hosts]) )

(refer-timbre)

(defn output-fn
  "output for repl result"
  ([data] (output-fn nil data))
  ([opts data] ; For partials
   (let [ {:keys [level ?err #_vargs msg_ ?ns-str ?file hostname_ timestamp_ ?line]} data]
     (str   (style "> " :blue) (force msg_)))))

(defn slf4j-fix []
  (let [cl (.getContextClassLoader  (Thread/currentThread))]
    (-> cl
      (.loadClass "org.slf4j.LoggerFactory")
      (.getMethod "getLogger"  (into-array java.lang.Class [(.loadClass cl "java.lang.String")]))
      (.invoke nil (into-array java.lang.Object ["ROOT"])))))

(defn disable-coloring
   "See https://github.com/ptaoussanis/timbre"
   []
  (merge-config! 
    {:output-fn (partial output-fn  {:stacktrace-fonts {}})})
  (merge-config!  
    {:appenders  {:rolling  (rolling-appender  {:path "supernal.log" :pattern :weekly})}}))

(defn setup-logging
  "Sets up logging configuration"
  []
  (slf4j-fix)
  (disable-coloring)
  (set-level! :info))

(setup-logging)

(defprotocol Report
 (summary [this target])
 (log- [this m])
 (pretty [this m]))

(extend-type Hosts
  Report
   (log- [this {:keys [success failure] :as m}]
     (info "Successful:")
     (doseq [{:keys [host out] :as m} success]
       (doseq [line out] (info ">" host ":" line)))
     (info "Failures:")
     (doseq [[code hosts] failure]
       (info code " >")
       (doseq [{:keys [host]} hosts]
         (info  "  " host)))
      [this m])

   (pretty [this {:keys [success failure] :as m}]
     (println "")
     (println "                 " (style "Successful:" :green :underline))
     (print-table success)
     (println "")
     (println "")
     (println "                 " (style "Failures:" :red :underline))
     (print-table [:host :code :error :out] (map (partial apply merge) (vals failure)))
     (println "")
     (println "")
     [this m]))

(defn refer-out []
  (require '[supernal.repl.output :as out :refer (log- pretty)]))
