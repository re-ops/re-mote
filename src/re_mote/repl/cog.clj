(ns re-mote.repl.cog
  "Running re-cog functions on hosts"
  (:require
   [com.rpl.specter :refer (ALL MAP-VALS transform select multi-path filterer)]
   [re-mote.zero.pipeline :refer (run-hosts)]
   [re-cog.plan :refer (execution-plan)]
   re-mote.repl.base)
  (:import [re_mote.repl.base Hosts]))

(defprotocol ReCog
  (run-inlined [this _ f args])
  (run-plan [this _ p args]))

(defn combine-success [{:keys [success failure] :as acc} f curr-success]
  (if success
    (let [host-results (into {} (map (fn [{:keys [result host]}] {host {f result}}) curr-success))]
      (mapv (fn [{:keys [host] :as v}] (update v :result (partial merge (host-results host)))) success))
    (transform [ALL :result] (fn [v] {f v}) curr-success)))

(defn combine-results [f {:keys [hosts] :as m} {:keys [failure success]}]
  (let [failing (into #{} (select [MAP-VALS ALL :host] failure))]
    (->  m
         (combine-success f success)
         (update :failure (fn [v] (merge-with conj (or v {}) failure)))
         (assoc :hosts (filter (comp not failing) hosts)))))

(defn run-recipe [args]
  "We run a recipe function and merge its results into the accumulated results which include:
    * The next hosts that are were successful in this round under :hosts
    * Combined failures under :failure
    * A combined success result per hosts with all of its results merged per recipe function
  "
  (fn [results f]
    (combine-results f results (run-hosts (results :hosts) (deref (resolve (symbol f))) args [5 :minute]))))

(extend-type Hosts
  ReCog
  (run-inlined [this _ f args]
    (let [f' (deref (resolve (symbol f)))]
      [this (run-hosts this f' args [5 :minute])]))
  (run-plan [this _ p args]
    (let [namespaces (deref (resolve (symbol p)))]
      (doseq [n namespaces]
        (require n))
      [this (reduce (run-recipe args) {:succesful (this :hosts)} (execution-plan namespaces))])))

(defn refer-cog []
  (require '[re-mote.repl.cog :as cog :refer (run-inlined run-plan)]))

