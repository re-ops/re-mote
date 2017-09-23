(ns re-mote.zero.functions
  (:require
   [cheshire.core :refer (parse-string)]
   [re-share.metrics :refer (read-metrics)]
   [clojure.java.shell :refer [sh]]
   [serializable.fn :as s]
   [me.raynes.fs :refer (list-dir)]))

(def ^{:doc "adding one"} plus-one
  (s/fn [x] (+ 1 x)))

(def ^{:doc "list dir"} ls
  (s/fn [d]
    (map str (list-dir d))))

(def ^{:doc "touch a file"} touch
  (s/fn [f]
    (touch f)))

(def ^{:doc "apt update"} apt-update
  (s/fn []
    (sh "sudo" "apt" "update")))

(def ^{:doc "always fails"} fails
  (s/fn []
    (sh "fail")))

(def ^{:doc "Getting all OS information using oshi"} oshi-os
  (s/fn []
    (get-in (read-metrics) [:operatingSystem])))

(def ^{:doc "Getting all Hardware information using oshi"} oshi-hardware
  (s/fn []
    (get-in (read-metrics) [:hardware])))

(def ^{:doc "Puppet facter facts"} facter
  (s/fn []
    (parse-string (:out (sh "facter" "--json")) true)))

(defn refer-zero-fns []
  (require '[re-mote.zero.functions :as fns :refer (apt-update fails touch plus-one oshi-os oshi-hardware)]))

(defn fn-meta [f]
  (meta
   (second
    (first
     (filter #(and (var? (second %)) (= f (var-get (second %)))) (ns-map 're-mote.zero.functions))))))

(comment
  (clojure.pprint/pprint (:hardware (read-metrics))))
