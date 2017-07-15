(ns re-mote.repl.re-gent
  "Copy .curve server public key and run agent remotly"
  (:require
   [re-mote.repl.base :refer (run-hosts)])
  (:import [re_mote.repl.base Hosts]))

(defprotocol Regent
  (launch
    [this]
    [this m]))

(extend-type Hosts
  Regent
  (launch [this])
  (launch [this m]))

(defn refer-regent []
  (require '[re-mote.repl.re-gent :as re-gent :refer (launch)]))
