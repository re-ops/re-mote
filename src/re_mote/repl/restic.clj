(ns re-mote.repl.restic
  "Restic https://restic.net/ operations"
  (:require
   [clojure.string :refer (upper-case)]
   [re-mote.ssh.pipeline :refer (run-hosts)]
   [clojure.core.strint :refer (<<)]
   [re-mote.repl.base :refer (refer-base)]
   [re-mote.repl.publish :refer (email)]
   [pallet.stevedore :refer (script)])
  (:import [re_mote.repl.base Hosts]))

(defn escape [s]
  (str \" s \"))

(defn prefix [t suffix]
  (<< "~(upper-case (name t))_~{suffix}"))

(defn backup-script
  "restic backup script"
  [{:keys [src dest pass id key type] :as b}]
  (let [target (<< "~{type}:~{dest}")]
    (script
     ("export" (set! RESTIC_PASSWORD ~(escape pass)))
     ("export" (set! ~(prefix type "ACCOUNT_KEY") ~(escape key)))
     ("export" (set! ~(prefix type "ACCOUNT_ID") ~(escape id)))
     ("/usr/bin/restic" "backup" ~src "-r" ~target))))

(defn check-script
  "restic backup script"
  [{:keys [dest pass id key type] :as b}]
  (let [target (<< "~{type}:~{dest}")]
    (script
     ("export" (set! RESTIC_PASSWORD ~(escape pass)))
     ("export" (set! ~(prefix type "ACCOUNT_KEY") ~(escape key)))
     ("export" (set! ~(prefix type "ACCOUNT_ID") ~(escape id)))
     ("/usr/bin/restic" "check" "-r" ~target))))

(defprotocol Restic
  (backup
    [this b]
    [this m b])
  (check
    [this b]
    [this m b]))

(extend-type Hosts
  Restic
  (check
    ([this b]
     (check this {} b))
    ([this m b]
     [this (run-hosts this (check-script b))]))
  (backup
    ([this b]
     (backup this {} b))
    ([this m b]
     [this (run-hosts this (backup-script b))])))

(defn refer-restic []
  (require '[re-mote.repl.restic :as rst :refer (backup check)]))
