(ns re-mote.repl.cog
  "Running re-cog functions on hosts"
  (:require
   [re-mote.zero.pipeline :refer (run-hosts)]
   re-mote.repl.base)
  (:import [re_mote.repl.base Hosts]))

(defprotocol ReCog
  (run-inlined [this _ f args]))

(extend-type Hosts
  ReCog
  (run-inlined
    ([this _ f args]
     [this (run-hosts this f args)])))

(defn refer-cog []
  (require '[re-mote.repl.cog :as cog :refer (run-inlined)]))

