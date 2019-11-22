(ns re-mote.zero.security
  "Security data collection detection"
  (:require
   [re-mote.zero.pipeline :refer (run-hosts)]
   [taoensso.timbre :refer (refer-timbre)]
   [re-cog.scripts.common :refer (shell-args)]
   [re-mote.zero.stats :refer (zip comma)]
   [re-cog.scripts.security :refer (ufw-script ssh-connections)]
   [re-cog.resources.exec :refer (shell)]
   [re-cog.facts.security :refer (scan-hosts scan-ports)]
   re-mote.repl.base)
  (:import [re_mote.repl.base Hosts]))

(refer-timbre)

(defprotocol Security
  (rules [this] [this m])
  (open-ports [this flags network] [this m flags network])
  (hosts [this flags network] [this m flags network])
  (ssh-sessions [this] [this m]))

(def timeout [5 :second])

(extend-type Hosts
  Security
  (open-ports [this flags network]
    [this (run-hosts this scan-ports ["/usr/bin/" flags network] [5 :minute])])
  (hosts [this flags network]
    [this (run-hosts this scan-hosts ["/usr/bin/" flags network] [5 :minute])])
  (ssh-sessions
    ([this]
     (zip this (run-hosts this shell (shell-args ssh-connections)) :security :ssh :proto :rcv-q :snd-q :local :remote :state :pid-name)))
  (rules
    ([this]
     (zip this (run-hosts this shell (shell-args ufw-script) timeout) :security :rules :from :action :to comma))))

(defn refer-security []
  (require '[re-mote.zero.security :as security :refer (rules open-ports)]))
