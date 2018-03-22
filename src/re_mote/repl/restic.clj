(ns re-mote.repl.restic
  "Restic https://restic.net/ operations"
  (:require
   [re-mote.ssh.pipeline :refer (run-hosts)]
   [clojure.core.strint :refer (<<)]
   [re-mote.repl.base :refer (refer-base)]
   [re-mote.repl.publish :refer (email)]
   [pallet.stevedore :refer (script)])
  (:import [re_mote.repl.base Hosts]))

(defn escape [s]
  (str \" s \"))

(defn restic-script
  "restic backup script"
  [{:keys [src dest pass id key type] :as b}]
  (let [target (<< "~{type}:~{dest}")]
    (script
      ("export" (set! RESTIC_PASSWORD ~(escape pass)))
      ("export" (set! B2_ACCOUNT_KEY ~(escape key)))
      ("export" (set! B2_ACCOUNT_ID ~(escape id)))
      ("/usr/bin/restic" "backup" ~src "-r" ~target))))

(defprotocol Restic
  (backup
    [this b]
    [this m b]))

(extend-type Hosts
  Restic
   (backup 
     ([this b]
       (backup this {} b))
     ([this m b]
       (println (restic-script b))
       [this (run-hosts this (restic-script b))])))

(defn refer-restic []
  (require '[re-mote.repl.restic :as rst :refer (backup)]))
