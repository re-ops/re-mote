(ns re-mote.repl.base
  (:require
   [clojure.java.shell :refer [sh]]
   [clojure.core.strint :refer (<<)]
   [clojure.edn :as edn]
   [clojure.tools.trace :as t]
   [clojure.java.io :refer (reader file)]
   [taoensso.timbre :refer (refer-timbre)]
   [hara.data.map :refer (dissoc-in)]
   [re-mote.ssh.pipeline :refer (run-hosts upload-hosts)]
   [pallet.stevedore.bash]
   [pallet.stevedore :refer (script)]))

(refer-timbre)

(defn sh-hosts
  "Run a local commands against hosts"
  [{:keys [auth hosts]} sh-fn]
  (let [results (map (fn [host] (assoc (sh-fn host) :host host)) hosts)
        grouped (group-by :code results)]
    {:hosts hosts :success (grouped 0) :failure (dissoc grouped 0)}))

(.bindRoot #'pallet.stevedore/*script-language* :pallet.stevedore.bash/bash)

(defmacro | [source fun & funs]
  (let [f (first fun) args (rest fun)]
    `(let [[this# res#] ~source]
       (~f this# res# ~@args))))

(defmacro run [f p s & fns]
  (if-not (empty? fns)
    `(run (~p ~f ~s) ~(first fns) ~(second fns) ~@(rest (rest fns)))
    `(~p ~f ~s)))

(defn safe-output [{:keys [out err exit]}]
  (when (seq out)
    (debug out))
  (when-not (zero? exit)
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

(defprotocol Select
  (initialize [this])
  (downgrade
    [this f]
    [this m f])
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
    [this (run-hosts this (script ("ls" ~flags ~target)))])

  (mkdir [this folder flags]
    [this (run-hosts this (script ("mkdir" ~flags ~folder)))])

  (rm [this target flags]
    [this (run-hosts this (script ("rm" ~flags ~target)))])

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

  (downgrade [this f]
     [this {}])

  (downgrade [this {:keys [failure] :as m} f]
    (let [failed (map :host (get failure -1))
          result (f (Hosts. auth failed))
          m' (clojure.core/update (dissoc-in m [:failure -1]) :success (fn [v] (println (result :success)) (into v (result :success))))] 
      (clojure.pprint/pprint m')
     [this m]))

  Tracing
  (ping [this target]
    [this (run-hosts this (script ("ping" "-c" 1 ~target)))]))

(defn successful
  "Used for picking successful"
  [success _ hs]
  (filter (set (map :host success)) hs))

(defn into-hosts
  "builds hosts from an edn file"
  [f]
  (let [{:keys [auth hosts]} (edn/read-string (slurp (file f)))]
    (Hosts. auth hosts)))

(defn refer-base []
  (require '[re-mote.repl.base :as base :refer (run | initialize pick successful ping ls into-hosts exec scp extract rm nohup mkdir sync- downgrade)]))

