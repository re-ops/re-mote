(ns re-mote.zero.git
  "Git actions"
  (:require
   [re-cog.scripts.common :refer (shell-args)]
   [re-cog.resources.exec :refer (shell)]
   [re-mote.zero.pipeline :refer (run-hosts)]
   [pallet.stevedore :refer (script)]
   re-mote.repl.base)
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
     [this (run-hosts this shell (shell-args (pull-script repo remote branch)) timeout)])))

(defn refer-git []
  (require '[re-mote.zero.git :as git]))
