(ns re-mote.zero.security
  "Security data collection detection"
  (:require
   [re-mote.zero.pipeline :refer (run-hosts)]
   [taoensso.timbre :refer (refer-timbre)]
   [re-mote.zero.shell :refer (args)]
   [re-mote.zero.stats :refer (zip comma)]
   [re-mote.scripts.security :refer (ufw-script)]
   [re-mote.zero.functions :refer (shell scan-hosts scan-ports)]
   re-mote.repl.base)
  (:import [re_mote.repl.base Hosts]))

(refer-timbre)

(defprotocol Security
  (rules [this] [this m])
  (open-ports [this flags network] [this m flags network])
  (hosts [this flags network] [this m flags network])
  (ssh-logins [this] [this m]))

(def timeout [5 :second])

(extend-type Hosts
  Security
  (open-ports [this flags network]
    [this (run-hosts this scan-ports ["/usr/bin/" flags network] [5 :minute])])
  (hosts [this flags network]
    [this (run-hosts this scan-hosts ["/usr/bin/" flags network] [5 :minute])])
  (rules
    ([this]
     (zip this (run-hosts this shell (args ufw-script) timeout) :security :rules :from :action :to comma))))

(defn refer-security []
  (require '[re-mote.zero.security :as security :refer (rules open-ports)]))
