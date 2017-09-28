(ns re-mote.repl.apt
  "APT Package automation"
  (:refer-clojure :exclude  [update])
  (:require
   [clojure.string :refer (split join)]
   [re-mote.log :refer (get-log)]
   [re-mote.repl.base :refer (run-hosts)]
   [pallet.stevedore :refer (script)])
  (:import [re_mote.repl.base Hosts]))

(defprotocol Apt
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
  (unlock
    ([this]
     (unlock this {}))

    ([this _]
     [this (run-hosts this (script ("sudo" "rm" "/var/lib/dpkg/lock /var/cache/apt/archives/lock")))]))

  (kill-apt
    ([this]
     (kill-apt this {}))

    ([this _]
     [this (run-hosts this (script ("sudo" "killall" "apt")))]))

  (install [this _ pkg]
    (install this pkg))

  (install [this pkg]
    [this (run-hosts this (script ("sudo" "apt" "install" ~pkg "-y")))]))

(defn refer-apt []
  (require '[re-mote.repl.apt :as apt :refer (install unlock kill-apt)]))
