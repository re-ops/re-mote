(ns re-mote.repl.base
  (:require
   [clojure.java.shell :refer [sh]]
   [clojure.core.strint :refer (<<)]
   [clojure.edn :as edn]
   [clojure.string :refer (split)]
   [clojure.tools.trace :as t]
   [clojure.core.async :refer (<!! thread thread-call) :as async]
   [clojure.java.io :refer (reader file)]
   [taoensso.timbre :refer (refer-timbre)]
   [pallet.stevedore.bash]
   [pallet.stevedore :refer (script)]
   [re-mote.log :refer (collect-log get-logs gen-uuid)]
   [re-mote.sshj :refer (execute upload)]))

(refer-timbre)

(.bindRoot #'pallet.stevedore/*script-language* :pallet.stevedore.bash/bash)

(defn- execute-uuid [auth script host]
  (try
    (let [uuid (gen-uuid)
          code (execute script (merge {:host host} auth) :out-fn (collect-log uuid))]
      {:host host :code code :uuid uuid})
    (catch Throwable e
      {:host host :code -1 :error (.getMessage e)})))

(defn- map-async
  "Map functions in seperate theads and merge the results"
  [f ms]
  (<!! (async/into [] (async/merge (map #(thread-call (bound-fn []  (f %))) ms)))))

(defn- host-upload [auth src dest h]
  (try
    (upload src dest (merge {:host h} auth))
    {:host h :code 0}
    (catch Throwable e
      {:host h :code 1 :error (.getMessage e)})))

(defn sh-hosts
  "Run a local commands against hosts"
  [{:keys [auth hosts]} sh-fn]
  (let [results (map (fn [host] (assoc (sh-fn host) :host host)) hosts)
        grouped (group-by :code results)]
    {:hosts hosts :success (grouped 0) :failure (dissoc grouped 0)}))

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

(defn safe-output [{:keys [out err exit]}]
  (when-not (empty? out)
    (debug out))
  (when-not (= exit 0)
    (error err exit))
  {:code exit :out out :error err})

(def safe (comp safe-output sh))

(defprotocol Shell
  (exec [this script])
  (nohup [this script])
  (rm
    [this target flags]
    [this m target flags])
  (ls [this target flags])
  (grep [this expr flags])
  (mkdir [this folder flags])
  (cp [this src dest flags]))

(defprotocol Tracing
  (ping [this target]))



(defprotocol Copy
  (scp
    [this src dest]
    [this m src dest])
  (sync-
    [this src dest]
    [this m src dest]))

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
  (pick
    [this f]
    [this m f]))

(defn rsync [src target host {:keys [user ssh-key]}]
  (let [opts (if ssh-key (<< "-ae 'ssh -i ~{ssh-key}'") "-a")
        dest (<< "~{user}@~{host}:~{target}")]
    (script ("rsync" "--delete" ~opts  ~src  ~dest))))

(defrecord Hosts [auth hosts]
  Shell
  (ls [this target flags]
    [this (run-hosts this (script ("ls" ~target ~flags)))])

  (mkdir [this folder flags]
    [this (run-hosts this (script ("mkdir" ~folder ~flags)))])

  (rm [this target flags]
    [this (run-hosts this (script ("rm" ~target ~flags)))])

  (rm [this _ target flags]
    (rm this target flags))

  (exec [this script]
    [this (run-hosts this script)])

  (nohup [this cmd]
    (exec this (<< "nohup sh -c '~{cmd} &' &>/dev/null")))

  Tar
  (extract [this _ archive target]
    [this (run-hosts this (script ("tar" "-xzf" ~archive "-C" ~target)))])

  Copy
  (scp [this _ src target]
    (scp this src target))
  (scp [this src target]
    [this (upload-hosts this src target)])

  (sync- [this _ src target]
    (sync- this src target))

  (sync- [{:keys [auth hosts] :as this} src target]
    [this (sh-hosts this (fn [host] (safe "bash" "-c" (rsync src target host auth))))])

  Select
  (initialize [this]
    [this hosts])

  (pick [this f]
    (Hosts. auth (filter f hosts)))

  (pick [this {:keys [failure success] :as m} f]
    (let [hs (f success failure hosts)]
      (if (empty? hs)
        (throw (ex-info "no succesful hosts found" m))
        [(Hosts. auth hs) {}])))

  Tracing
  (ping [this target]
    [this (run-hosts this (script ("ping" "-c" 1 ~target)))])
 )

(defn successful
  "Used for picking successful"
  [success _ hs] (filter (into #{} (map :host success)) hs))

(defn into-hosts
  "builds hosts from an edn file"
  [f]
  (let [{:keys [auth hosts]} (edn/read-string (slurp (file f)))]
    (Hosts. auth hosts)))

(defn refer-base []
  (require '[re-mote.repl.base :as base :refer (run | initialize pick successful ping ls into-hosts exec scp extract rm nohup mkdir sync-)]))

