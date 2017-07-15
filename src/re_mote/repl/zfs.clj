(ns re-mote.repl.zfs
  "A bunch of function for ZFS automation"
  (:require
   [clj-time.format :as f]
   [clj-time.local :refer [local-now]]
   [clojure.core.strint :refer (<<)]
   [pallet.stevedore :refer (script)]
   [re-mote.repl.output :refer (refer-out)]
   [re-mote.repl.base :refer (refer-base)]))

(refer-base)
(refer-out)

(defn scrub [hs pool]
  (run (exec hs (<< "sudo /sbin/zpool scrub ~{pool}")) | (pretty)))

(def errors "'(DEGRADED|FAULTED|OFFLINE|UNAVAIL|REMOVED|FAIL|DESTROYED|corrupt|cannot|unrecover)'")

(defn healty [pool errors]
  (script
   (pipe ("/sbin/zpool" "status" ~pool) ("egrep" "-v" "-i" ~errors))))

(defn cap-with-range [maximum]
  (script
   (set! used @("/sbin/zpool" "list" "-H" "-o" "capacity" | "cut" "-d'%'" "-f1"))
   (if (>= @used ~maximum)
     (chain-and (println "used capacity is too high" @used "maximum allowed is" ~maximum) ("exit" 1))
     ("exit" 0))))

(defn purging
  "purge script"
  [pool dataset n]
  (let [n+ (str "+" n) from (str pool "/" dataset)]
    (script
     (pipe
      (pipe ("zfs" "list" "-H" "-t" "snapshot" "-o" "name" "-S" "creation" "-d1" ~from) ("tail" "-n" ~n+))
      ("xargs" "-r" "-n" "1" "zfs" "destroy" "-r")))))

(defn health [hs pool]
  (run (exec hs (healty pool errors)) | (pretty)))

(defn capacity [hs maximum]
  (run (exec hs (cap-with-range maximum)) | (pretty)))

(defn snapshot [hs pool dataset]
  (let [date (f/unparse (f/formatter "dd-MM-YYYY_hh:mm:ss_SS") (local-now))]
    (run (exec hs (<< "/sbin/zfs snapshot ~{pool}/~{dataset}@~{date}")) | (pretty))))

(defn purge
  "clear last n snapshots of a dataset"
  [hs pool dataset n]
  (run (exec hs (purging pool dataset n)) | (pretty)))

(defn refer-zfs []
  (require '[re-mote.repl.zfs :as zfs :refer (health snapshot scrub capacity purge)]))
