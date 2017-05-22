(ns re-mote.repl.octo
  "Basic octo (git backup tool)"
  (:require 
    [clojure.core.strint :refer (<<)]
    [re-mote.repl.base :refer (refer-base)]
    [re-mote.repl.publish :refer (email)]
    [pallet.stevedore :refer (script)]))

(refer-base)

(defn octo-script [f]
  (script
    (chain-and 
      ("/usr/bin/octo" "sync" ~f) 
      ("/usr/bin/octo" "push" ~f)
      ("/usr/bin/octo" "pull" ~f))))

(defn full-cycle [host f tofrom]
  (run (exec host (octo-script f)) | (email (tofrom (<< "octo ~{f} sync")))))

(defn refer-octo []
  (require '[re-mote.repl.octo :as octo :refer (full-cycle)]))
