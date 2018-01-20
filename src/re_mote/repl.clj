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
   [re-mote.zero.security :refer (refer-security)]
   [re-mote.zero.sensors :refer (refer-sensors)]
   [re-mote.repl.re-gent :refer (refer-regent)]
   [re-share.schedule :refer (watch seconds)]
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
(refer-security)
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
  (run (ls hs "/" "-la") | (pretty "listing")))

; alerting

(defn #^{:category :detection} inactive-firewall
  "find inactive firewall"
  [hs]
  (run (rules hs) | (pick (fn [_ failure] (mapv :host (failure 1))))))

(defn #^{:category :detection} low-disk
  "Detect machines with low disk available"
  [hs f]
  (run (du hs) | (detect f)))

; persistent stats

(defn #^{:category :stats} du-persist
  "Disk usage"
  [hs]
  (run (du hs) | (persist "stats")))

(defn #^{:category :stats} cpu-persist
  "CPU usage and idle stats collection and persistence"
  [hs]
  (run (cpu hs) | (persist "stats")))

(defn #^{:category :stats} ram-persist
  "RAM free and used percentage collection and persistence"
  [hs]
  (run (free hs) | (persist "stats")))

(defn #^{:category :stats} net-persist
  "KB in/out stats collection and persistence"
  [hs]
  (run (net hs) | (persist "stats")))

(defn #^{:category :stats} temperature-persist
  "Collect CPU temperature (using lm-sensors) and publish"
  [hs]
  (run (temperature hs) | (persist "stats")))

(defn #^{:category :stats} load-persist
  "Average load collection and persistence"
  [hs]
  (run (load-avg hs) | (persist "stats")))

(defn tofrom
  "Email configuration"
  [desc]
  {:to "narkisr@gmail.com" :from "gookup@gmail.com" :subject (<< "Running ~{desc} results")})

; Packaging
(defn #^{:category :packaging} update
  "Apt update on hosts"
  [hs]
  (run (pkg/update hs) | (pretty "update") | (email (tofrom "package update"))))

(defn #^{:category :packaging} upgrade
  "Apt update and upgrade on hosts, only update successful hosts gets upgraded"
  [hs]
  (run (pkg/update hs) | (pick successful) | (pkg/upgrade) | (pretty "upgrade") | (email (tofrom "package upgrade"))))

(defn #^{:category :packaging} install
  "Install a package on hosts"
  [hs pkg]
  (run (pkg/install hs pkg) | (pretty "package install")))

(defn #^{:category :packaging} pakage-fix
  "Fix package provider"
  [hs]
  (run (pkg/fix hs) | (pkg/kill) | (pretty "package provider fix")))

; Puppet
(defn #^{:category :puppet} copy-module
  "Copy an opskeleton tar file"
  [hs pkg]
  (let [name (.getName (file pkg))]
    (run (scp hs pkg "/tmp") | (pretty "scp") | (pick successful)
         | (extract (<< "/tmp/~{name}") "/tmp") | (rm (<< "/tmp/~{name}") "-rf"))))

(defn #^{:category :puppet} run-module
  "Run an opskeleton sandbox"
  [hs pkg args]
  (let [[this _] (copy-module hs pkg)  extracted (.replace (.getName (file pkg)) ".tar.gz" "")]
    (run (apply-module this (<< "/tmp/~{extracted}") args) | (pretty "run module") | (rm (<< "/tmp/~{extracted}") "-rf"))))

(defn #^{:category :puppet} provision
  "Sync puppet source code into a VM and run"
  [hs {:keys [src]}]
  {:pre [src]}
  (let [dest (<< "/tmp/~(fs/base-name src)")]
    (run (rm hs dest "-rf") | (sync- src dest) | (pick successful) | (apply-module dest "") | (pretty "provision"))))

; re-gent
(defn #^{:category :re-gent} deploy
  "deploy re-gent and setup .curve remotely:
     (deploy sandbox \"path/to/re-gent\")"
  [{:keys [auth] :as hs} bin]
  (let [{:keys [user]} auth home (<< "/home/~{user}") dest (<< "~{home}/.curve")]
    (run (mkdir hs dest "-p") | (scp ".curve/server-public.key" dest) | (pretty "curve copy"))
    (run (kill-agent hs) | (pretty "kill agnet"))
    (run (scp hs bin home) | (pick successful) | (start-agent home) | (pretty "scp"))))

(defn #^{:category :re-gent} kill
  "kill re-gent process:
     (kill develop)"
  [{:keys [auth] :as hs}]
  (run (kill-agent hs) | (pretty "kill agent")))

(defn #^{:category :re-gent} launch
  "start re-gent process:
     (launch develop)
  "
  [{:keys [auth] :as hs}]
  (let [{:keys [user]} auth home (<< "/home/~{user}")]
    (run (start-agent hs home) | (pretty "launch agent"))))

(defn pull
  "update a local git repo"
  [hs {:keys [repo remote branch]}]
  (run (git/pull hs repo remote branch) | (pretty "git pull")))

(defn filter-hosts
  [hs f]
  (run (os-info hs) | (pick (partial results-filter f)) | (pretty "filter hosts")))

; sanity testing

(defn failing [hs]
  (run (tst/listdir hs "/") | (pick successful) | (tst/fail) | (pretty "failing")))
