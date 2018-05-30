(ns re-mote.repl.re-conf
  "Running re-conf recpies on hosts"
  (:require
   [clojure.string :refer (join)]
   [re-mote.ssh.pipeline :refer (run-hosts)]
   [pallet.stevedore :refer (script)])
  (:import [re_mote.repl.base Hosts]))

(defprotocol ReConf
  (apply-recipes
    [this path args]
    [this m path args]))

(defn reconf-script [path args]
  (script
   ("cd" ~path)
   ("sudo" "/usr/bin/node" "main.js" ~args)))

(extend-type Hosts
  ReConf
  (apply-recipes
    ([this path args]
     (apply-recipes this nil path args))
    ([this _ path args]
     [this (run-hosts this (reconf-script path (join " " args)))])))

(defn refer-reconf []
  (require '[re-mote.repl.re-conf :as reconf :refer (apply-recipes)]))

