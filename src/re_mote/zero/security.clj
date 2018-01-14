(ns re-mote.zero.security
  "Security data collection detection"
  (:require
   [clojure.core.strint :refer (<<)]
   [re-mote.zero.pipeline :refer (run-hosts)]
   [taoensso.timbre :refer (refer-timbre)]
   [re-mote.zero.shell :refer (args)]
   [re-mote.scripts.security :refer (ufw-script)]
   [re-mote.zero.functions :refer (shell)]
   )
  (:import [re_mote.repl.base Hosts])
 )
(defprotocol Security
  (firewall [this] [this m])
  (ssh-logins [this] [this m])
  )
