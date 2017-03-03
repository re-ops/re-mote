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

(ns supernal.repl
  "Repl utilities for supernal"
  (:require
    [clojure.string :as s]
    [taoensso.timbre :refer (refer-timbre set-level! merge-config!)]
    [pallet.stevedore.bash]
    [pallet.stevedore :refer (with-script-language script)]
    [supernal.sshj :refer (execute collect-log get-log gen-uuid)]))

(.bindRoot #'pallet.stevedore/*script-language* :pallet.stevedore.bash/bash)

(set-level! :info)
(refer-timbre)

(defn output-fn
  "Default (fn [data]) -> string output fn.
  Use`(partial default-output-fn <opts-map>)` to modify default opts."
  ([data] (output-fn nil data))
  ([opts data] ; For partials
   (let [{:keys [no-stacktrace? stacktrace-fonts]} opts
         {:keys [level ?err #_vargs msg_ ?ns-str ?file hostname_
                 timestamp_ ?line]} data]
     (str (s/upper-case (name level))  " " (force msg_)
       ))))

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

(defprotocol Shell
  (rm [this hosts target flags])
  (ls [this hosts target flags])
  (grep [this hosts expr flags])
  (cp [this hosts src dest flags]))

(defprotocol Stats
  (cpu [this hosts]))

(defprotocol Tracing
  (ping [this hosts target]))

(defprotocol Report
 (summary [this target])
 (log- [this m])
 (pretty [this m])
 )

(defprotocol Select
  (initialize [this])
  (pick [this m f])
  )

(defn execute-uuid [auth script host]
  (try
    (let [uuid (gen-uuid)
          code (execute script {:host host :user (auth :user)} :out-fn (collect-log uuid))]
       {:host host :code code :uuid uuid})
    (catch Throwable e
       {:host host :code :fail :error (.getMessage e)})))

(defn get-logs [hosts]
   (map (fn [{:keys [uuid] :as m}] (assoc m :out (get-log uuid))) hosts))

(defn run-hosts [auth hosts script]
  (let [results (map (partial execute-uuid auth script ) hosts)
          grouped (group-by :code results)]
      {:hosts hosts :success (merge (get-logs (grouped 0))) :failure (dissoc grouped 0)}))

(defrecord Hosts [auth]
  Tracing
  (ping [this {:keys [hosts]} target]
    [this (run-hosts auth hosts (script ("ping" "-c" 1 ~target)))])

  Shell
  (ls [this {:keys [hosts]} target flags]
   [this (run-hosts auth hosts (script ("ls" ~target ~flags)))])

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
     [this (select-keys m [:hosts])])

  Select
  (initialize [this]
    [this {:hosts ["192.168.2.25" "192.168.2.26" "192.168.2.27"]}])

  (pick [this {:keys [hosts failure success]} f]
    [this {:hosts (filter (partial f success failure) hosts)}])

   Stats
    (cpu [this {:keys [hosts]}]
      (let [stat (script (pipe ("mpstat" "2" "1") ("awk" "'{ print $12 }'") ("tail" "-1")))]
        [this (run-hosts auth hosts stat)])))

(defmacro | [source fun & funs]
  (let [f (first fun) args (rest fun)]
     `(let [[this# res#] ~source]
        (~f this# res# ~@args))))

(defmacro run [f p s & fns]
  (if-not (empty? fns)
    `(run (~p ~f ~s) ~(first fns) ~(second fns) ~@(rest (rest fns)))
    `(~p ~f ~s)))

(def hosts (Hosts. {:user "vagrant"}))

;; (run (initialize hosts) | (ping "google.com") | (log-) | (ls "/home/vagrant/" "-la") | (log-))
;; (run (initialize hosts) | (cpu) | (pretty))
