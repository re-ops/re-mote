(ns re-mote.repl
  "Repl utilities for re-mote"
  (:refer-clojure :exclude  [update])
  (:require
   [me.raynes.fs :as fs]
   [clojure.core.strint :refer (<<)]
   [re-mote.validate :refer (check-entropy check-jce)]
   [clojure.pprint :refer (pprint)]
   [taoensso.timbre :refer (refer-timbre)]
   [re-mote.repl.base :refer (refer-base)]
   [re-mote.persist.es :refer (refer-es-persist)]
   [re-mote.repl.zfs :refer (refer-zfs)]
   [re-mote.repl.output :refer (refer-out)]
   [re-mote.repl.publish :refer (refer-publish)]
   [re-mote.repl.puppet :refer (refer-puppet)]
   [re-mote.repl.octo :refer (refer-octo)]
   [re-mote.zero.stats :refer (refer-stats)]
   [re-mote.zero.sensors :refer (refer-sensors)]
   [re-mote.repl.re-gent :refer (refer-regent)]
   [re-mote.repl.schedule :refer (watch seconds)]
   [re-mote.zero.facts :refer (refer-facts)]
   [re-mote.zero.git :refer (refer-git)]
   [re-mote.zero.test :as tst]
   [re-mote.zero.pkg :refer (refer-pkg)]
   [re-mote.log :refer (setup-logging)]
   [clojure.java.io :refer (file)])
  (:import [re_mote.repl.base Hosts]))

(refer-timbre)
(refer-facts)
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
(refer-git)
(refer-es-persist)

(defn setup []
  (check-entropy 200)
  (check-jce)
  (setup-logging)
  (setup-stats 10 10))

(defn single [h & m]
  (Hosts. (merge {:user "upgrade"} (first m)) [h]))

(def develop (Hosts. {:user "vagrant"} ["re-a" "re-b"]))

(def localhost (Hosts. {:user "upgrade"} ["rosetta"]))

(def methone (Hosts. {:user "upgrade"} ["methone"]))

(def bsd (Hosts. {:user "upgrade"} ["re-e"]))

(defn #^{:category :shell} listing [hs]
  (run (ls hs "/" "-la") | (pretty)))

; PUBLISHING
(defn #^{:category :stats} cpu-publish
  "CPU usage and idle stats collection and publishing"
  [hs]
  (run (cpu hs) | (collect) | (publish (stock "Idle CPU" :timeseries :idle)) | (publish (stock "User CPU" :timeseries :usr))))

(defn #^{:category :stats} cpu-persist
  "CPU usage and idle stats collection and publishing"
  [hs]
  (run (cpu hs) | (persist "stats") | (pretty)))

(defn #^{:category :stats} ram-publish
  "RAM free and used percentage collection and publishing"
  [hs]
  (run (free hs) | (collect) | (publish (stock "Free RAM" :timeseries :free)) | (publish (stock "Used RAM" :timeseries :used))))

(defn #^{:category :stats} net-publish
  "KB in/out stats collection and publishing"
  [hs]
  (run (net hs) | (collect) | (publish (stock "KB out" :timeseries :txkB/s)) | (publish (stock "KB in" :timeseries :rxkB/s))))

(defn #^{:category :stats} temperature-publish
  "Collect CPU temperature (using lm-sensors) and publish"
  [hs]
  (run (temperature hs) | (collect) | (publish (stock "CPU temp" :timeseries :coretemp-isa-0000 0 :temp))))

(defn #^{:category :stats} load-publish
  "Average load collection and publishing"
  [hs]
  (run (load-avg hs) | (collect) | (publish (stock "Five load" :timeseries :five))))

(defn inlined-stats [hs]
  (run (free hs) | (collect) | (cpu) | (collect) | (sliding avg :avg) | (publish (stock "User cpu avg" :avg :usr))))

(defn tofrom
  "Email configuration"
  [desc]
  {:to "narkisr@gmail.com" :from "gookup@gmail.com" :subject (<< "Running ~{desc} results")})

; Packaging
(defn #^{:category :packaging} update
  "Apt update on hosts"
  [hs]
  (run (pkg/update hs) | (pretty) | (email (tofrom "package update"))))

(defn #^{:category :packaging} upgrade
  "Apt update and upgrade on hosts, only update successful hosts gets upgraded"
  [hs]
  (run (pkg/update hs) | (pick successful) | (pkg/upgrade) | (pretty) | (email (tofrom "package upgrade"))))

(defn #^{:category :packaging} install
  "Install a package on hosts"
  [hs pkg]
  (run (pkg/install hs pkg) | (pretty)))

(defn #^{:category :packaging} pakage-fix
  "Fix package provider"
  [hs]
  (run (pkg/fix hs) | (pkg/kill) | (pretty)))

; Puppet
(defn #^{:category :puppet} copy-module
  "Copy an opskeleton tar file"
  [hs pkg]
  (let [name (.getName (file pkg))]
    (run (scp hs pkg "/tmp") | (pretty) | (pick successful)
         | (extract (<< "/tmp/~{name}") "/tmp") | (rm (<< "/tmp/~{name}") "-rf"))))

(defn #^{:category :puppet} run-module
  "Run an opskeleton sandbox"
  [hs pkg args]
  (let [[this _] (copy-module hs pkg)  extracted (.replace (.getName (file pkg)) ".tar.gz" "")]
    (run (apply-module this (<< "/tmp/~{extracted}") args) | (pretty) | (rm (<< "/tmp/~{extracted}") "-rf"))))

(defn #^{:category :puppet} provision
  "Sync puppet source code into a VM and run"
  [hs {:keys [src]}]
  {:pre [src]}
  (let [dest (<< "/tmp/~(fs/base-name src)")]
    (run (rm hs dest "-rf") | (sync- src dest) | (pick successful) | (apply-module dest "") | (pretty))))

; re-gent
(defn #^{:category :re-gent} deploy
  "deploy re-gent and setup .curve remotely:
     (deploy sandbox \"path/to/re-gent\")
  "
  [{:keys [auth] :as hs} bin]
  (let [{:keys [user]} auth home (<< "/home/~{user}") dest (<< "~{home}/.curve")]
    (run (mkdir hs dest "-p") | (scp ".curve/server-public.key" dest) | (pretty))
    (run (kill-agent hs) | (pretty))
    (run (scp hs bin home) | (pick successful) | (start-agent home) | (pretty))))

(defn #^{:category :re-gent} kill
  "kill re-gent process:
     (kill develop)
  "
  [{:keys [auth] :as hs}]
  (run (kill-agent hs) | (pretty)))

(defn #^{:category :re-gent} launch
  "start re-gent process:
     (launch develop)
  "
  [{:keys [auth] :as hs}]
  (let [{:keys [user]} auth home (<< "/home/~{user}")]
    (run (start-agent hs home) | (pretty))))

(defn pull
  "update a local git repo"
  [hs {:keys [repo remote branch]}]
  (run (git/pull hs repo remote branch) | (pretty)))

(defn filter-hosts
  [hs f]
  (run (os-info hs) | (pick (partial results-filter f)) | (pretty)))

; sanity testing

(defn failing [hs]
  (run (tst/listdir hs "/") | (pick successful) | (tst/fail) | (pretty)))
