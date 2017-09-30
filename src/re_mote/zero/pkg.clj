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
      [this (run-hosts this pkg-upgrade [] [5 :minute])])))

(defn refer-pkg []
  (require '[re-mote.zero.pkg :as pkg]))

