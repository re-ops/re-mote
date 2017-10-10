(ns re-mote.zero.shell
  "Common shell (bash) functions"
  (:require
   [re-share.core :refer (md5)]
   [pallet.stevedore :refer (script do-script)]))

(defn bash!
  "check that we are running within bash!"
  []
  (script
   ("[" "!" "-n" "\"$BASH\"" "]" "&&" "echo" "Please set default user shell to bash" "&&" "exit" 1)))

(defn validate!
  "validating a bash script"
  [f]
  (do-script (bash!) (f)))

(defn args
  "script args"
  [bs]
  [(md5 (bs)) (validate! bs)])
