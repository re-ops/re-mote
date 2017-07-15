(ns re-mote.repl
  "Repl utilities for re-mote"
  (:refer-clojure :exclude  [update])
  (:require
   [clojure.core.strint :refer (<<)]
   [re-mote.validate :refer (check-entropy check-jce)]
   [clojure.pprint :refer (pprint)]
   [taoensso.timbre :refer (refer-timbre)]
   [re-mote.repl.base :refer (refer-base)]
   [re-mote.repl.zfs :refer (refer-zfs)]
   [re-mote.repl.output :refer (refer-out)]
   [re-mote.repl.pkg :refer (refer-pkg)]
   [re-mote.repl.publish :refer (refer-publish)]
   [re-mote.repl.puppet :refer (refer-puppet)]
   [re-mote.repl.octo :refer (refer-octo)]
   [re-mote.repl.stats :refer (refer-stats)]
   [re-mote.repl.sensors :refer (refer-sensors)]
   [re-mote.repl.re-gent :refer (refer-regent)]
   [re-mote.repl.schedule :refer (watch seconds)]
   [re-mote.log :refer (setup-logging)]
   [clojure.java.io :refer (file)])
  (:import [re_mote.repl.base Hosts]))

(refer-timbre)
(refer-base)
(refer-out)
(refer-stats)
(refer-sensors)
(refer-pkg)
(refer-puppet)
(refer-zfs)
(refer-publish)
(refer-octo)
(refer-regent)

(defn setup []
  (check-entropy 200)
  (check-jce)
  (setup-logging)
  (setup-stats 10 10))

(def sandbox (Hosts. {:user "vagrant"} ["192.168.2.28" "192.168.2.26" "192.168.2.27"]))

(def localhost (Hosts. {:user "upgrade"} ["localhost"]))

(defn listing [hs]
  (run (ls hs "/" "-la") | (pretty)))

; PUBLISHING
(defn cpu-publish [hs]
  (run (cpu hs) | (collect) | (publish (stock "Idle CPU" :timeseries :idle)) | (publish (stock "User CPU" :timeseries :usr))))

(defn ram-publish [hs]
  (run (free hs) | (collect) | (publish (stock "Free RAM" :timeseries :free)) | (publish (stock "Used RAM" :timeseries :used))))

(defn net-publish [hs]
  (run (net hs) | (collect) | (publish (stock "KB out" :timeseries :txkB/s)) | (publish (stock "KB in" :timeseries :rxkB/s))))

(defn temperature-publish [hs]
  (run (temperature hs) | (collect) | (publish (stock "CPU temp" :timeseries :coretemp-isa-0000 0 :temp))))

(defn load-publish [hs]
  (run (load-avg hs) | (collect) | (publish (stock "Five load" :timeseries :five))))

(defn inlined-stats [hs]
  (run (free hs) | (collect) | (cpu) | (collect) | (sliding avg :avg) | (publish (stock "User cpu avg" :avg :usr))))

(defn tofrom [desc]
  {:to "narkisr@gmail.com" :from "gookup@gmail.com" :subject (<< "Running ~{desc} results")})

; Packaging
(defn aptdate [hs]
  (run (update hs) | (pretty) | (email (tofrom "apt update"))))

(defn aptgrade [hs]
  (run (aptdate hs) | (pick successful) | (upgrade) | (pretty) | (email (tofrom "apt upgrade"))))

(defn add-package [hs pkg]
  (run (install hs pkg) | (pretty)))

(defn apt-rewind
  "try to put apt back on track"
  [hs]
  (run (unlock hs) | (kill-apt) | (pretty) | (email (tofrom "apt rewind"))))

; Puppet
(defn copy-module [hs pkg]
  (let [name (.getName (file pkg))]
    (run (scp hs pkg "/tmp") | (pretty) | (pick successful)
         | (extract (<< "/tmp/~{name}") "/tmp") | (rm (<< "/tmp/~{name}") "-rf"))))

(defn run-module [hs pkg args]
  (let [[this _] (copy-module hs pkg)  extracted (.replace (.getName (file pkg)) ".tar.gz" "")]
    (run (apply-module this (<< "/tmp/~{extracted}") args) | (pretty) | (rm (<< "/tmp/~{extracted}") "-rf"))))

(defn provision
  "Sync puppet source code into a VM and run"
  [hs src dest]
  (run (rm hs dest "-rf") | (sync- src dest) | (pick successful) | (apply-module dest "") | (pretty)))

; re-gent

(defn deploy-agent [hs bin]
  (run (mkdir hs "/tmp/.curve" "") | (pretty) | (scp ".curve/server-public.key" "/tmp/.curve") | (pretty))
  (run (scp hs bin "/tmp") | (pretty) | (pick successful) | (launch)))
