(ns re-mote.zero.pipeline
  "Base ns for zeromq pipeline support"
  (:require
   [clojure.set :refer (rename-keys)]
   [re-mote.spec :as re-spec :refer (valid?)]
   [clojure.core.match :refer [match]]
   [taoensso.timbre :refer  (refer-timbre)]
   [com.rpl.specter :refer (transform MAP-VALS ALL VAL)]
   [re-mote.zero.management :refer (refer-zero-manage)]
   [re-mote.zero.results :refer (refer-zero-results)]
   [re-mote.zero.functions :refer (call)]
   [re-cog.core :refer (fn-meta)]
   [re-mote.zero.cycle :refer (ctx)]
   [re-share.core :refer (wait-for)]))

(refer-timbre)
(refer-zero-manage)
(refer-zero-results)

(defn codes [v]
  (match [v]
    [{:result {:out _}}] -1
    [{:result {:exit e}}] e
    :else 0))

(defn with-codes
  [m uuid]
  (transform [ALL] (fn [[h v]] [h (merge {:host h :code (codes v) :uuid uuid} v)]) m))

(defn collect
  "Collect results from the zmq hosts blocking until all results are back or timeout end"
  [hosts k uuid timeout]
  (try
    (wait-for {:timeout timeout :sleep [100 :ms]}
              (fn [] (get-results hosts uuid)) "Failed to collect all hosts")
    (catch Exception e
      (warn "Failed to get results"
            (merge (ex-data e)
                   {:missing (missing-results hosts uuid) :k k :uuid uuid}))))
  (let [rs (with-codes (get-results hosts uuid) uuid)]
    (clear-results uuid)
    rs))

(defn non-reachable
  "Adding non reachable hosts"
  [{:keys [hosts]} up uuid]
  (into {}
        (map
         (fn [h] [h {:code -1 :host h :error {:out "host re-gent not connected"} :uuid uuid}])
         (filter (comp not (partial contains? up)) hosts))))

(defn add-error [errors]
  (transform [MAP-VALS ALL] (fn [m] (rename-keys m {:result :error})) errors))

(defn run-hosts
  ([hs f args]
   (run-hosts hs f args [10 :second]))
  ([hs f args timeout]
   {:post [(valid? ::re-spec/operation-result %)]}
   (let [hosts (into-zmq-hosts hs)
         uuid (call f args hosts)
         results (collect (keys hosts) (-> f fn-meta :name keyword) uuid timeout)
         down (non-reachable hs hosts uuid)
         grouped (group-by :code (vals (merge results down)))
         success (or (grouped 0) [])
         failure (add-error (dissoc grouped 0))]
     {:hosts (keys hosts) :success success :failure failure})))

(defn refer-zero-pipe []
  (require '[re-mote.zero.pipeline :as zpipe :refer (collect run-hosts)]))

(comment
  (send- (send-socket @ctx) {:address 1234 :content {:request :execute}}))
