(ns re-mote.zero.git
  "Git actions"
  (:require
   [re-mote.zero.shell :refer (args)]
   [re-mote.zero.functions :refer (shell)]
   [re-mote.zero.pipeline :refer (run-hosts)]
   [pallet.stevedore :refer (script)])
  (:import [re_mote.repl.base Hosts]))

(defprotocol Git
  (pull
    [this repo remote branch]
    [this m repo remote branch]))

(def timeout [2 :second])

(defn pull-script [repo remote branch]
  (fn [] (script ("git" "--git-dir" ~repo "pull" ~remote ~branch))))

(extend-type Hosts
  Git
  (pull
    ([this repo remote branch]
     (pull this {} repo remote branch))
    ([this _ repo remote branch]
     [this (run-hosts this shell (args (pull-script repo remote branch)) timeout)])))

(defn refer-git []
  (require '[re-mote.zero.git :as git]))
