(ns re-mote.zero.pipeline
  "Base ns for zeromq pipeline support"
  (:require
   [clojure.core.match :refer [match]]
   [taoensso.timbre :refer  (refer-timbre)]
   [com.rpl.specter :refer (transform MAP-VALS ALL)]
   [re-mote.zero.management :refer (refer-zero-manage)]
   [re-mote.zero.results :refer (refer-zero-results)]
   [re-mote.zero.functions :as fns :refer (fn-meta call)]
   [re-mote.zero.cycle :refer (ctx)]
   [re-share.core :refer (wait-for)]))

(refer-timbre)
(refer-zero-manage)
(refer-zero-results)

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
  [hosts k uuid timeout]
  (try
    (wait-for {:timeout timeout :sleep [100 :ms]}
              (fn [] (get-results hosts uuid)) "Failed to collect all hosts")
    (catch Exception e
      (warn "Failed to get results"
            (merge (ex-data e) {:missing (missing-results hosts uuid) :k k :uuid uuid}))))
  (let [rs (with-codes (get-results hosts uuid) uuid)]
    (clear-results uuid)
    rs))

(defn non-reachable
  "Adding non reachable hosts"
  [{:keys [hosts]} up uuid]
  (into {}
        (map
         (fn [h] [h {:code -1 :host h :result {:r :failed} :out "host re-gent not connected" :uuid uuid}])
         (filter (comp not (partial contains? up)) hosts))))

(defn run-hosts
  ([hs f args]
   (run-hosts hs f args [10 :second]))
  ([hs f args timeout]
   (let [hosts (into-zmq-hosts hs)
         uuid (call f args hosts)
         results (collect (keys hosts) (-> f fn-meta :name keyword) uuid timeout)
         down (non-reachable hs hosts uuid)
         grouped (group-by :code (vals (merge results down)))]
     {:hosts hs :success (grouped 0) :failure (dissoc grouped 0)})))

(defn refer-zero-pipe []
  (require '[re-mote.zero.pipeline :as zpipe :refer (collect)]))

(comment
  (send- (send-socket @ctx) {:address 1234 :content {:request :execute}}))
