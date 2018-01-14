(ns re-mote.zero.security
  "Security data collection detection"
  (:require
   [clojure.core.strint :refer (<<)]
   [re-mote.zero.pipeline :refer (run-hosts)]
   [taoensso.timbre :refer (refer-timbre)]
   [re-mote.zero.shell :refer (args)]
   [re-mote.zero.stats :refer (zip)]
   [re-mote.scripts.security :refer (ufw-script)]
   [re-mote.zero.functions :refer (shell)])
  (:import [re_mote.repl.base Hosts]))

(refer-timbre)

(defprotocol Security
  (ports [this] [this m])
  (ssh-logins [this] [this m]))

(def timeout [5 :second])

(extend-type Hosts
  Security
  (ports
    ([this]
     (zip this
          (run-hosts this shell (args ufw-script) timeout)
          :security :ports :from :action :to))))

