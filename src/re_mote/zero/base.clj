(ns re-mote.zero.base
  "Base ns for zeromq pipeline support"
  (:require
   [com.rpl.specter :refer (transform MAP-VALS ALL)]
   [re-mote.zero.management :refer (refer-zero-manage)]
   [re-mote.zero.functions :as fns :refer (fn-meta)]
   [re-mote.zero.server :refer [send-]]
   [re-mote.log :refer (gen-uuid)]
   [re-share.core :refer (wait-for)]))

(refer-zero-manage)

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

(defn- get-results [{:keys [hosts]} k uuid]
  (let [rs (map (partial result uuid k) hosts)]
    (when (every? identity rs) (zipmap hosts rs))))

(defn with-codes
  [m uuid]
  (transform [ALL] (fn [[h v]] [h {:host h :code (if (= v :failed) -1 0)  :uuid uuid :result v}]) m))

(defn collect
  "Collect results from the zmq hosts blocking until all results are back"
  [hs k uuid timeout]
  (wait-for {:timeout timeout}
    (fn [] (get-results hs k uuid)) "Failed to collect all hosts")
  (with-codes
    (get-results hs k uuid) uuid))

(defn run-hosts
  ([hosts f args]
   (run-hosts hosts f args [10 :second]))
  ([hosts f args timeout]
   (let [uuid (call f args hosts)
         results (collect hosts (-> f fn-meta :name keyword) uuid timeout)
         grouped (group-by :code (vals results))]
     {:hosts hosts :success (grouped 0) :failure (dissoc grouped 0)})))

(defn refer-zero-base []
  (require '[re-mote.zero.base :as zbase :refer (call collect)]))

