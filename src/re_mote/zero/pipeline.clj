(ns re-mote.zero.pipeline
  "Base ns for zeromq pipeline support"
  (:require
   [clojure.core.match :refer [match]]
   [taoensso.timbre :refer  (refer-timbre)]
   [com.rpl.specter :refer (transform MAP-VALS ALL)]
   [re-mote.zero.management :refer (refer-zero-manage)]
   [re-mote.zero.results :refer (refer-zero-results)]
   [re-mote.zero.functions :as fns :refer (fn-meta)]
   [re-mote.log :refer (gen-uuid)]
   [re-mote.zero.send :refer (send-)]
   [re-mote.zero.cycle :refer (ctx)]
   [re-share.core :refer (wait-for)]))

(refer-timbre)
(refer-zero-manage)
(refer-zero-results)

(defn call
  "Launch a remote clojure function on hosts
   The function has to support serialization"
  [f args hosts]
  (let [uuid (gen-uuid) zhs (into-zmq-hosts hosts)]
    (doseq [[hostname address] zhs]
      (send- address {:request :execute :uuid  uuid :fn f :args args :name (-> f fn-meta :name)}))
    (if (empty? zhs)
      (throw (ex-info "no registered hosts found!" {:hosts hosts}))
      uuid)))

(defn codes [v]
  (match [v]
    [{:r :failed}] -1
    [{:r {:exit e}}] e
    :else 0))

(defn with-codes
  [m uuid]
  (transform [ALL] (fn [[h v]] [h {:host h :code (codes v)  :uuid uuid :result v}]) m))

(defn collect
  "Collect results from the zmq hosts blocking until all results are back or timeout end"
  [hs k uuid timeout]
  (try
    (wait-for {:timeout timeout :sleep [100 :ms]}
              (fn [] (get-results hs uuid)) "Failed to collect all hosts")
    (catch Exception e
      (warn "Failed to get results"
            (merge (ex-data e) {:missing (missing-results hs uuid) :k k :uuid uuid}))))
  (let [rs (with-codes (get-results hs uuid) uuid)]
    (clear-results uuid)
    rs))

(defn run-hosts
  ([hosts f args]
   (run-hosts hosts f args [10 :second]))
  ([hosts f args timeout]
   (let [uuid (call f args hosts)
         results (collect hosts (-> f fn-meta :name keyword) uuid timeout)
         grouped (group-by :code (vals results))]
     {:hosts hosts :success (grouped 0) :failure (dissoc grouped 0)})))

(defn refer-zero-pipe []
  (require '[re-mote.zero.pipeline :as zpipe :refer (call collect)]))

(comment
  (send- (send-socket @ctx) {:address 1234 :content {:request :execute}}))
