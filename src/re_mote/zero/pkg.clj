(ns re-mote.zero.pkg
  "Package operations on FreeBSD Pkg and Ubuntu/Debian Apt"
  (:refer-clojure :exclude  [update])
  (:require
   [taoensso.timbre :refer  (refer-timbre)]
   [re-mote.zero.pipeline :refer (run-hosts)]
   [re-cog.resources.package :as pkg :refer (package)]
   re-mote.repl.base)
  (:import [re_mote.repl.base Hosts]))

(refer-timbre)

(defprotocol Pkg
  (update
    [this]
    [this m])
  (upgrade
    [this]
    [this m])
  (install
    [this pkg]
    [this m pkg]))

(extend-type Hosts
  Pkg
  (update
    ([this _]
     (update this))
    ([this]
     [this (run-hosts this pkg/update- [] [2 :minute])]))

  (upgrade
    ([this _]
     (upgrade this))
    ([this]
     [this (run-hosts this pkg/upgrade [] [5 :minute])]))

  (install
    ([this pkg]
     (install this {} pkg))
    ([this m pkg]
     [this (run-hosts this package [pkg :present] [5 :minute])])))

(defn refer-zero-pkg []
  (require '[re-mote.zero.pkg :as zpkg]))

