(ns re-mote.repl.pkg
  "Package automation"
  (:refer-clojure :exclude  [update])
  (:require
   [re-mote.repl.base :refer (run> nohup |)]
   [re-mote.repl.output :refer (pretty)]
   [clojure.string :refer (split join)]
   [re-mote.log :refer (get-log)]
   [re-mote.ssh.pipeline :refer (run-hosts)]
   [pallet.stevedore :refer (script)])
  (:import [re_mote.repl.base Hosts]))

(defprotocol Pkg
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

(def update-script
  (script
   (set! DEBIAN_FRONTEND "noninteractive")
   ("sudo" "apt" "update")))
(extend-type Hosts
  Pkg
  (update [this _]
    (update this))

  (update [this]
    [this (run-hosts this update-script)])

  (upgrade [this _]
    (upgrade this))

  (upgrade [this]
    [this (run-hosts this (script ("sudo" "apt" "upgrade" "-y")))])

  (unlock [this _]
    [this (run-hosts this (script ("sudo" "rm" "/var/lib/dpkg/lock /var/cache/apt/archives/lock")))])

  (kill-apt [this _]
    [this (run-hosts this (script ("sudo" "killall" "apt")))])

  (install
    ([this _ pkg]
     (install this pkg))
    ([this pkg]
     [this (run-hosts this (script ("sudo" "apt" "install" ~pkg "-y")))])))

(defn update-mirror
  "Update apt mirror"
  [hs]
  (run> (nohup hs "sudo /usr/bin/apt-mirror") | (pretty "apt-mirror updated")))

(defn refer-pkg []
  (require '[re-mote.repl.pkg :as pkg]))
