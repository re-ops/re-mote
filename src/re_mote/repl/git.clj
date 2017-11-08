(ns re-mote.repl.git
  (:require
   [re-mote.log :refer (get-log)]
   [re-mote.ssh.pipeline :refer (run-hosts)]
   [pallet.stevedore :refer (script)])
  (:import [re_mote.repl.base Hosts]))

(defprotocol Git
  (pull
    [this repo remote branch]
    [this m repo remote branch]))

(extend-type Hosts
  Git
  (pull
    ([this repo remote branch]
     (pull this {} repo remote branch))
    ([this _ repo remote branch]
     [this (run-hosts this (script ("git" "--git-dir" ~repo "pull" ~remote ~branch)))])))

(defn refer-git []
  (require '[re-mote.repl.git :as git :refer (pull)]))
