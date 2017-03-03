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
    [clojure.string :as s]
    [taoensso.timbre :refer (refer-timbre set-level! merge-config!)])
  (:import [supernal.repl.base Hosts]) )

(refer-timbre)

(defn output-fn
  "Default (fn [data]) -> string output fn.
  Use`(partial default-output-fn <opts-map>)` to modify default opts."
  ([data] (output-fn nil data))
  ([opts data] ; For partials
   (let [{:keys [no-stacktrace? stacktrace-fonts]} opts
         {:keys [level ?err #_vargs msg_ ?ns-str ?file hostname_
                 timestamp_ ?line]} data]
     (str (s/upper-case (name level))  " " (force msg_)))))

(defn disable-coloring
   "See https://github.com/ptaoussanis/timbre"
   []
  (merge-config! {:output-fn (partial output-fn  {:stacktrace-fonts {}})}))

(defn setup-logging
  "Sets up logging configuration"
  []
  (disable-coloring)
  (set-level! :info))

(setup-logging)
 


(defprotocol Report
 (summary [this target])
 (log- [this m])
 (pretty [this m]))

(extend-type  Hosts
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
      [this (select-keys m [:hosts])]
     )

   (pretty [this {:keys [success failure] :as m}]
     (info "Successful:")
     (doseq [{:keys [host out] :as m} success]
       (clojure.pprint/pprint m))
     (info "Failures")
     (doseq [fail failure]
       (clojure.pprint/pprint fail))
     [this (select-keys m [:hosts])]))
 
