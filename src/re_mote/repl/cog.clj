(ns re-mote.repl.cog
  "Running re-cog functions on hosts"
  (:require
   [com.rpl.specter :refer (ALL MAP-VALS select multi-path filterer)]
   [re-mote.zero.pipeline :refer (run-hosts)]
   [re-cog.plan :refer (execution-plan)]
   re-mote.repl.base)
  (:import [re_mote.repl.base Hosts]))

(defprotocol ReCog
  (run-inlined [this _ f args])
  (run-plan [this _ p args]))

(defn run-step [this args {:keys [succesful] :as m} f]
  "We run the function on the last succesful hosts, failing hosts are filtered out from the result."
  (let [f' (deref (resolve (symbol f)))
        next-hosts (update this :hosts succesful)
        {:keys [failure success] :as current} (run-hosts next-hosts f' args [5 :minute])
        failing (into #{} (select [MAP-VALS :host] failure))]
    (->  m
         (update :failure (partial merge-with conj))
         (update :succesful (filter (comp not failing) succesful)))))

(extend-type Hosts
  ReCog
  (run-inlined [this _ f args]
    (let [f' (deref (resolve (symbol f)))]
      [this (run-hosts this f' args [5 :minute])]))
  (run-plan [this _ p args]
    (let [namespaces (deref (resolve (symbol p)))]
      (doseq [n namespaces]
        (require n))
      (let [steps (execution-plan namespaces)
            results (reduce (partial run-step this args) {:succesful (this :hosts)})]
        [this]))))

(defn refer-cog []
  (require '[re-mote.repl.cog :as cog :refer (run-inlined run-plan)]))

