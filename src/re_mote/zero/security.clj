(ns re-mote.zero.security
  "Security data collection detection"
  (:require
   [com.rpl.specter :as s :refer (transform MAP-VALS ALL keypath)]
   [clojure.core.strint :refer (<<)]
   [re-mote.zero.pipeline :refer (run-hosts)]
   [taoensso.timbre :refer (refer-timbre)]
   [re-mote.zero.shell :refer (args)]
   [re-mote.zero.stats :refer (zip comma)]
   [re-mote.scripts.security :refer (ufw-script)]
   [re-mote.zero.functions :refer (shell run-scan)])
  (:import [re_mote.repl.base Hosts]))

(refer-timbre)

(defprotocol Security
  (rules [this] [this m])
  (scan [this flags network] [this m flags network])
  (ssh-logins [this] [this m]))

(def timeout [5 :second])

(extend-type Hosts
  Security
  (scan [this flags network]
    [this (run-hosts this run-scan ["/usr/bin/" flags network] [5 :minute])])
  (rules
    ([this]
     (zip this (run-hosts this shell (args ufw-script) timeout) :security :rules :from :action :to comma))))

(defn refer-security []
  (require '[re-mote.zero.security :as security :refer (rules scan)]))
