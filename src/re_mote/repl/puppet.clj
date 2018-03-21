(ns re-mote.repl.puppet
  "Remote puppet run of opsk sandboxes"
  (:require
   [clojure.string :refer (join)]
   [re-mote.ssh.pipeline :refer (run-hosts)]
   [pallet.stevedore :refer (script)])
  (:import [re_mote.repl.base Hosts]))

(defprotocol Puppet
  (apply-module
    [this path args]
    [this m path args]))

(defn puppet-script [path args]
  (script
   ("cd" ~path)
   (chain-or ("sudo" "/bin/bash" "run.sh" ~args "--detailed-exitcodes" "--color=false") ("[ $? -eq 2 ]"))))

(extend-type Hosts
  Puppet
  (apply-module
    ([this path args]
     (apply-module this nil path args))
    ([this _ path args]
     [this (run-hosts this (puppet-script path (join " " args)))])))

(defn refer-puppet []
  (require '[re-mote.repl.puppet :as puppet :refer (apply-module)]))
