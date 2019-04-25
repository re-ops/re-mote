(ns re-mote.zero.process
  "Process query and manipulation"
  (:require
   [re-mote.zero.pipeline :refer (run-hosts)]
   [re-mote.zero.functions :refer (all-processes processes-by named)]
   re-mote.repl.base)
  (:import [re_mote.repl.base Hosts]))

(defprotocol Processes
  (processes [this] [this targer]))

(extend-type Hosts
  Processes
  (processes
    ([this target]
     [this (run-hosts this processes-by [(named target)] [10 :second])])
    ([this]
     [this (run-hosts this all-processes [] [10 :second])])))

(defn refer-process []
  (require '[re-mote.zero.process :as process :refer (processes)]))
