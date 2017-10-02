(ns re-mote.zero.pkg
  "Package operations on FreeBSD Pkg and Ubuntu/Debian Apt"
  (:require
   [taoensso.timbre :refer  (refer-timbre)]
   [re-mote.zero.base :refer (run-hosts)]
   [re-mote.zero.functions :refer (refer-zero-fns)])
  (:import [re_mote.repl.base Hosts]))

(refer-timbre)
(refer-zero-fns)

(defprotocol Pkg
  (update
    [this]
    [this m])
  (upgrade
    [this]
    [this m])
  (install
    [this pkg]
    [this m pkg])
  (fix
    [this]
    [this m])
  (kill
    [this]
    [this m]))

(extend-type Hosts
  Pkg
  (update
    ([this _]
     (update this))
    ([this]
     [this (run-hosts this pkg-update [] [2 :minute])]))

  (upgrade
    ([this]
     (upgrade this {}))
    ([this m]
     [this (run-hosts this pkg-upgrade [] [5 :minute])]))

  (install
    ([this pkg]
     (install this {} pkg))
    ([this m pkg]
     [this (run-hosts this pkg-install [pkg] [5 :minute])]))

  (fix
    ([this]
     (fix this {}))
    ([this m]
     ([this (run-hosts this pkg-fix [] [1 :minute])])))

  (kill
    ([this]
     (kill this {}))
    ([this m]
     ([this (run-hosts this pkg-kill [] [1 :minute])]))))

(defn refer-pkg []
  (require '[re-mote.zero.pkg :as pkg]))

