(ns re-mote.repl.cog
  "Running re-cog functions on hosts"
  (:require
   [com.rpl.specter :refer (ALL MAP-VALS transform select multi-path filterer)]
   [re-mote.zero.pipeline :refer (run-hosts)]
   [re-cog.plan :refer (execution-plan)]
   re-mote.repl.base)
  (:import [re_mote.repl.base Hosts]))

(defprotocol ReCog
  (run-plan [this _ p args]))

(defn purge-failing
  "Purge failing hosts from the success list (they had previouse successes and now they failed"
  [{:keys [failure] :as m}]
  (let [failing-hosts (into #{} (select [MAP-VALS ALL :host] failure))]
    (update m :success (partial filter (comp not failing-hosts :host)))))

(defn combine-success
  "Combine the current run success output with the accumulated success value"
  [f {:keys [success]}]
  (fn [combined-success]
    (if combined-success
      (let [last-result (into {} (map (fn [{:keys [host] :as m}] {host m}) success))]
        (mapv
         (fn [{:keys [host] :as v}]
           (let [{:keys [result profile]} (last-result host)]
             (-> v
                 (update :result (fn [r] (conj r (assoc result :f f))))
                 (update :profile (partial merge-with + profile))))) combined-success))
      (transform [ALL :result] (fn [v] [(assoc v :f f)]) success))))

(defn combine-results
  "Combine current run result with the accumulated values"
  [f {:keys [hosts] :as m} {:keys [failure] :as curr-run}]
  (let [failing (into #{} (select [MAP-VALS ALL :host] failure))]
    (->  m
         (update :success (combine-success f curr-run))
         (update :failure (fn [v] (merge-with into (or v {}) failure)))
         (assoc :hosts (filter (comp not failing) hosts))
         purge-failing)))

(defn run-recipe [args]
  "We run a recipe function and merge its results into the accumulated results which include:
    * The next hosts that are were successful in this round under :hosts
    * Combined failures under :failure
    * A combined success result per hosts with all of its results merged per recipe function
  "
  (fn [results f]
    (combine-results (keyword (symbol f)) results (run-hosts results (deref (resolve (symbol f))) args [5 :minute]))))

(extend-type Hosts
  ReCog
  (run-plan [this _ p args]
    (let [namespaces (deref (resolve (symbol p)))]
      (doseq [n namespaces]
        (require n))
      [this (reduce (run-recipe args) {:hosts (:hosts this)} (execution-plan namespaces))])))

(defn refer-cog []
  (require '[re-mote.repl.cog :as cog :refer (run-plan)]))

