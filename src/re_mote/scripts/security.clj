(ns re-mote.scripts.security
  "Security scripts"
  (:require
   [pallet.stevedore :refer (script do-script)]))

(defn ufw-script []
  (script
   (set! R @("sudo" "ufw" "status"))
   (if (not (= $? 0)) ("exit" 1))
   (when (= @R "Status: inactive") ("echo" "firewall disabled!") ("exit" 1))
   (pipe (pipe (pipe ((println (quoted "${R}"))) ("awk" "'NR>4'")) ("sed -r 's/  +/,/g'")) ("tr" "-s" "' '"))))
