(ns supernal.repl.base
  (:require
    [clojure.java.io :refer (reader)]
    [taoensso.timbre :refer (refer-timbre )]
    [pallet.stevedore.bash]
    [pallet.stevedore :refer (script)]
    [supernal.sshj :refer (execute collect-log get-log gen-uuid)]))

(refer-timbre)

(.bindRoot #'pallet.stevedore/*script-language* :pallet.stevedore.bash/bash)

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

(defmacro | [source fun & funs]
  (let [f (first fun) args (rest fun)]
     `(let [[this# res#] ~source]
        (~f this# res# ~@args))))

(defmacro run [f p s & fns]
  (if-not (empty? fns)
    `(run (~p ~f ~s) ~(first fns) ~(second fns) ~@(rest (rest fns)))
    `(~p ~f ~s)))

(defprotocol Shell
  (rm [this hosts target flags])
  (ls [this hosts target flags])
  (grep [this hosts expr flags])
  (cp [this hosts src dest flags]))

(defprotocol Tracing
  (ping [this hosts target]))

(defprotocol Select
  (initialize [this])
  (pick [this m f]))
 
(defrecord Hosts [auth initial]

  Shell
  (ls [this {:keys [hosts]} target flags]
   [this (run-hosts auth hosts (script ("ls" ~target ~flags)))])

  Select
   (initialize [this]
    [this {:hosts initial}])

  (pick [this {:keys [hosts failure success]} f]
    [this {:hosts (filter (partial f success failure) hosts)}])

  Tracing
  (ping [this {:keys [hosts]} target]
    [this (run-hosts auth hosts (script ("ping" "-c" 1 ~target)))]))

(defn refer-base []
  (require '[supernal.repl.base :as base :refer (run | initialize pick ping ls)]))
