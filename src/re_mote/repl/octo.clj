(ns re-mote.repl.octo
  "Basic octo (git backup tool)"
  (:require 
    [re-mote.repl.base :refer (refer-base)]
    [re-mote.repl.publish :refer (email)]
    [pallet.stevedore :refer (script)]))

(refer-base)

(defn octo-script []
  (script
    (chain-and 
      ("/usr/bin/octo" "sync" "/etc/octo.edn") 
      ("/usr/bin/octo" "push" "/etc/octo.edn")
      ("/usr/bin/octo" "pull" "/etc/octo.edn"))))

(defn full-cycle [host tofrom]
  (run (exec host (octo-script)) | (email (tofrom "octo sync"))))

(defn refer-octo []
  (require '[re-mote.repl.octo :as octo :refer (full-cycle)]))
