(ns re-mote.zero.functions
  (:require
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

(def ^{:doc "running processes"} processes
   (s/fn []
     (get-in (read-metrics) [:operatingSystem :processes])))

(defn fn-meta [f]
  (meta
   (second
     (first
       (filter #(and (var? (second %)) (= f (var-get (second %)))) (ns-map 're-mote.zero.functions))))))

