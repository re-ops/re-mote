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

(ns re-mote.repl.base
  (:require
    [clojure.core.strint :refer (<<)]
    [clojure.edn :as edn]
    [clojure.string :refer (split)]
    [clojure.tools.trace :as t]
    [clojure.core.async :refer (<!! thread thread-call) :as async]
    [clojure.java.io :refer (reader file)]
    [taoensso.timbre :refer (refer-timbre )]
    [pallet.stevedore.bash]
    [pallet.stevedore :refer (script)]
    [re-mote.log :refer (collect-log get-logs gen-uuid)]
    [re-mote.sshj :refer (execute upload)]))

(refer-timbre)

(.bindRoot #'pallet.stevedore/*script-language* :pallet.stevedore.bash/bash)

(defn execute-uuid [auth script host]
  (try
    (let [uuid (gen-uuid)
          code (execute script (merge {:host host} auth) :out-fn (collect-log uuid))]
       {:host host :code code :uuid uuid})
    (catch Throwable e
       {:host host :code :fail :error (.getMessage e)})))

(defn map-async
  "Map functions in seperate theads and merge the results"
  [f ms]
   (<!! (async/into [] (async/merge (map #(thread-call (bound-fn []  (f %))) ms)))))

(defn host-upload [auth src dest h]
  (try 
     (upload src dest (merge {:host h} auth))
     {:host h :code 0}
    (catch Throwable e
      {:host h :code 1 :error (.getMessage e)})))

(defn upload-hosts [{:keys [auth hosts]} src dest]
  (let [results (map-async (partial host-upload auth src dest) hosts)
        grouped (group-by :code results)]
      {:hosts hosts :success (grouped 0) :failure (dissoc grouped 0)}))

(defn run-hosts [{:keys [auth hosts]} script]
  (let [results (map-async (partial execute-uuid auth script) hosts)
        grouped (group-by :code results)]
      {:hosts hosts :success (grouped 0) :failure (dissoc grouped 0)}))

(defmacro | [source fun & funs]
  (let [f (first fun) args (rest fun)]
     `(let [[this# res#] ~source]
        (~f this# res# ~@args))))

(defmacro run [f p s & fns]
  (if-not (empty? fns)
    `(run (~p ~f ~s) ~(first fns) ~(second fns) ~@(rest (rest fns)))
    `(~p ~f ~s)))

(defprotocol Shell
  (exec [this script])
  (nohup [this script])
  (rm [this m target flags])
  (ls [this target flags])
  (grep [this expr flags])
  (cp [this src dest flags]))

(defprotocol Tracing
  (ping [this target]))

(defprotocol Copy
  (scp [this src dest]))

(defprotocol Tar
  (extract [this m archive target]))

(defprotocol Performance
  (measure [this m]))

(defn zip
  "Collecting output into a hash, must be defined outside protocoal because of var args"
  [this {:keys [success failure] :as res} parent k & ks]
    (let [zipped (fn [{:keys [out] :as m}] (assoc-in m [parent k] (zipmap ks (split out #"\s"))))
          success' (map zipped (get-logs success))
          failure' (into {} (map (fn [[code rs]] [code (get-logs rs)]) failure))]
      [this (assoc (assoc res :success success') :failure failure')]))

(defprotocol Select
  (initialize [this])
  (pick [this m f]))

(defrecord Hosts [auth hosts]
  Shell
  (ls [this target flags]
    [this (run-hosts this (script ("ls" ~target ~flags)))])

  (rm [this _ target flags]
    [this (run-hosts this (script ("rm" ~target ~flags)))]
    )

  (exec [this script]
    [this (run-hosts this script)])

  (nohup [this cmd]
    (exec this (<< "nohup sh -c '~{cmd} &' &>/dev/null")))

  Tar
  (extract [this _ archive target]
     [this (run-hosts this (script ("tar" "-xzf" ~archive "-C" ~target)))])

  Copy
   (scp [this src target]
      [this (upload-hosts this src target)])

  Select
   (initialize [this]
    [this hosts])

  (pick [this {:keys [failure success] :as m} f]
    (let [hs (f success failure hosts)]
      (if (empty? hs)
        (throw (ex-info "no succesful hosts found" m))
        [(Hosts. auth hs) {}])))

  Tracing
  (ping [this target]
    [this (run-hosts this (script ("ping" "-c" 1 ~target)))]))

(defn successful
  "Used for picking successful"
  [success _ hs] (filter (into #{} (map :host success)) hs))

(defn into-hosts
   "builds hosts from an edn file"
   [f]
   (let [{:keys [auth hosts]} (edn/read-string (slurp (file f)))]
     (Hosts. auth hosts)))

(defn refer-base []
  (require '[re-mote.repl.base :as base :refer (run | initialize pick successful ping ls into-hosts exec scp extract rm nohup)]))

