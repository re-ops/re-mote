(ns re-mote.zero.security
  "Security data collection detection"
  (:require
   [com.rpl.specter :as s :refer (transform MAP-VALS ALL keypath)]
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
  (rules [this] [this m])
  (ssh-logins [this] [this m]))

(def timeout [5 :second])

(defn trim-dash [v]
  (if (.endsWith v "_") (subs v 0 (- (.length v) 1)) v))

(defn from-trim
  [[this m]]
  [this (transform [:success ALL :security :rules ALL :from] trim-dash  m)])

(extend-type Hosts
  Security
  (rules
    ([this]
     (from-trim
      (zip this (run-hosts this shell (args ufw-script) timeout) :security :rules :from :action :to)))))

(defn refer-security []
  (require '[re-mote.zero.security :as security :refer (rules)]))
