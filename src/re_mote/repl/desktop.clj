(ns re-mote.repl.desktop
  "Desktop oriented operations"
  (:require
   [re-mote.ssh.pipeline :refer (run-hosts)]
   [clojure.core.strint :refer (<<)]
   [re-mote.repl.base :refer (refer-base)]
   [re-mote.repl.publish :refer (email)]
   [pallet.stevedore :refer (script)]
   re-mote.repl.base)
  (:import [re_mote.repl.base Hosts]))

(defprotocol Desktop
  (browse
    [this url]
    [this m url]))

(defn firefox-script
  "Launch Firefox"
  [url]
  (script
   ("export" (set! DISPLAY ":0"))
   ("nohup" "sh" "-c" "'/usr/bin/firefox" ~url "&'")))

(extend-type Hosts
  Desktop
  (browse
    ([this url]
     (browse this nil url))
    ([this _ url]
     [this (run-hosts this (firefox-script url))])))

(defn refer-desktop []
  (require '[re-mote.repl.desktop :as dsk :refer (browse)]))
