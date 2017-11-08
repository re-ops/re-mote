(ns re-mote.ssh.pipeline
  (:require
   [re-mote.repl.spec :as re-spec]
   [clojure.spec.alpha :as spec]
   [clojure.core.strint :refer (<<)]
   [me.raynes.fs :as fs]
   [re-mote.ssh.transport :refer (execute upload)]
   [re-mote.log :refer (collect-log get-logs gen-uuid)]
   [clojure.core.async :refer (<!! thread-call) :as async]))

(defn- execute-uuid [auth script host]
  (let [uuid (gen-uuid)]
    (try
      (let [code (execute script (merge {:host host} auth) :out-fn (collect-log uuid))]
        {:host host :code code :uuid uuid})
      (catch Throwable e
        {:host host :code -1 :error {:out (.getMessage e)} :uuid uuid}))))

(defn- map-async
  "Map functions in seperate theads and merge the results"
  [f ms]
  (<!! (async/into [] (async/merge (map #(thread-call (bound-fn []  (f %))) ms)))))

(defn- host-upload [auth src dest h]
  (let [uuid (gen-uuid)]
    (try
      (upload src dest (merge {:host h} auth))
      {:host h :code 0 :uuid uuid}
      (catch Throwable e
        {:host h :code 1 :error {:out (.getMessage e)} :uuid uuid}))))

(defn upload-hosts [{:keys [auth hosts]} src dest]
  {:post [(spec/valid? ::re-spec/operation-result %)]}
  (when-not (fs/exists? src)
    (throw (ex-info (<< "missing source file to upload ~{src}") {:src src})))
  (let [results (map-async (partial host-upload auth src dest) hosts)
        grouped (group-by :code results)]
    {:hosts hosts :success (grouped 0) :failure (dissoc grouped 0)}))

(defn run-hosts [{:keys [auth hosts]} script]
  {:post [(spec/valid? ::re-spec/operation-result %)]}
  (let [results (map-async (partial execute-uuid auth script) hosts)
        grouped (group-by :code results)]
    {:hosts hosts :success (grouped 0) :failure (dissoc grouped 0)}))

