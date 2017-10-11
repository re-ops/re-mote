(ns re-mote.zero.test
  "basic operation like ls/fail etc.. (mainly for testing)"
  (:require
   [taoensso.timbre :refer  (refer-timbre)]
   [re-mote.zero.pipeline :refer (run-hosts)]
   [re-mote.zero.functions :refer (refer-zero-fns)])
  (:import [re_mote.repl.base Hosts]))

(refer-zero-fns)

(defprotocol Test
  (ls
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
     [this (run-hosts this fails [] [1 :second])]))

  (ls
    ([this target]
     (ls this target {}))
    ([this target m]
     [this (run-hosts this listdir [target] [1 :second])]))
  )

(defn refer-base []
  (require '[re-mote.zero.test :as ztest]))
