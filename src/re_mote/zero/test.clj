(ns re-mote.zero.test
  "basic operation like ls/fail etc.. (mainly for testing)"
  (:require
   [taoensso.timbre :refer  (refer-timbre)]
   [re-mote.zero.pipeline :refer (run-hosts)]
   [re-mote.zero.functions :as fns])
  (:import [re_mote.repl.base Hosts]))

(defprotocol Test
  (listdir
    [this target]
    [this target m])
  (fail
    [this]
    [this m]))

(extend-type Hosts
  Test
  (fail
    ([this _]
     (fail this))
    ([this]
     [this (run-hosts this fns/fails [] [1 :second])]))

  (listdir
    ([this target]
     (listdir this target {}))
    ([this target m]
     [this (run-hosts this fns/listdir [target] [1 :second])])))

(defn refer-base []
  (require '[re-mote.zero.test :as ztest]))
