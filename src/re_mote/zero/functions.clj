(ns re-mote.zero.functions
  (:require 
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

(defn fn-meta [f]
  (meta
    (second (first (filter #(and (var? (second %)) (= f (var-get (second %)))) 
	(ns-map 're-mote.zero.functions))))))
 
