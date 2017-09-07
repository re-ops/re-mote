(ns re-mote.zero.facts
  "Stats metadata etc.."
  (:require
   [re-mote.zero.base :refer (refer-zero-base)]
   [re-mote.zero.functions :refer (refer-zero-fns)])
  (:import [re_mote.repl.base Hosts]))

(refer-zero-base)
(refer-zero-fns)

(defn run-hosts [{:keys [hosts]} f args]
  (let [uuid (call f args hosts)
        results (collect hosts f uuid [5 :minute])
        grouped (group-by :code results)]
    {:hosts hosts :success (grouped 0) :failure (dissoc grouped 0)}))

(defprotocol Facts
  (os-type [this]))

#_(extend-type Hosts
    Facts
    (os-type [{:keys [hosts] :as m}]
      (let [uuid (call os-meta [] hosts)]
        [this {}])))

(defn refer-facts []
  (require '[re-mote.zero.facts :as facts :refer (os-type)]))
