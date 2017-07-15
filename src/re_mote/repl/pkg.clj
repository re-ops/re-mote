(ns re-mote.repl.pkg
  "Package automation"
  (:refer-clojure :exclude  [update])
  (:require
   [clojure.string :refer (split join)]
   [re-mote.log :refer (get-log)]
   [re-mote.repl.base :refer (run-hosts)]
   [pallet.stevedore :refer (script)])
  (:import [re_mote.repl.base Hosts]))

(defprotocol Apt
  (update
    [this]
    [this m])
  (upgrade
    [this]
    [this m])
  (unlock
    [this]
    [this m])
  (kill-apt
    [this]
    [this m])
  (install
    [this pkg]
    [this m pkg]))

(extend-type Hosts
  Apt
  (update [this _]
    (update this))

  (update [this]
    [this (run-hosts this (script ("sudo" "apt" "update")))])

  (upgrade [this _]
    [this (run-hosts this (script ("sudo" "apt" "upgrade" "-y")))])

  (unlock [this _]
    [this (run-hosts this (script ("sudo" "rm" "/var/lib/dpkg/lock /var/cache/apt/archives/lock")))])

  (kill-apt [this _]
    [this (run-hosts this (script ("sudo" "killall" "apt")))])

  (install
    ([this _ pkg] (install this pkg))
    ([this pkg] [this (run-hosts this (script ("sudo" "apt" "install" ~pkg "-y")))])))

(defn refer-pkg []
  (require '[re-mote.repl.pkg :as pkg :refer (update upgrade install unlock kill-apt)]))
