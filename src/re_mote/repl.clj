(ns re-mote.repl
  "Main remote workflow functions of Re-mote, it includes functions for performing a range of operations from updating packages to running an Nmap scan and collecting metrics.
   For more info check https://re-ops.github.io/re-ops/"
  (:refer-clojure :exclude  [update])
  (:require
   [me.raynes.fs :as fs]
   [clojure.core.strint :refer (<<)]
   [re-mote.repl.cog :refer (refer-cog)]
   [re-mote.validate :refer (check-entropy check-jce)]
   [clojure.pprint :refer (pprint)]
   [taoensso.timbre :refer (refer-timbre)]
   [re-mote.repl.base :refer (refer-base)]
   [re-mote.persist.es :refer (refer-es-persist)]
   [re-mote.repl.desktop :refer (refer-desktop)]
   [re-mote.repl.zfs :refer (refer-zfs)]
   [re-mote.repl.stress :refer (refer-stress)]
   [re-mote.repl.output :refer (refer-out)]
   [re-mote.repl.publish :refer (refer-publish)]
   [re-mote.repl.spec :refer (refer-spec)]
   [re-mote.repl.octo :refer (refer-octo)]
   [re-mote.repl.restic :refer (refer-restic)]
   [re-mote.zero.stats :refer (refer-stats)]
   [re-mote.zero.security :refer (refer-security)]
   [re-mote.zero.sensors :refer (refer-zero-sensors)]
   [re-mote.repl.re-gent :refer (refer-regent)]
   [re-mote.zero.facts :refer (refer-facts)]
   [re-mote.zero.osquery :refer (refer-osquery)]
   [re-mote.zero.process :refer (refer-process)]
   [re-mote.zero.git :refer (refer-git)]
   [re-mote.zero.test :as tst]
   [re-mote.zero.pkg :refer (refer-zero-pkg)]
   [re-mote.repl.pkg :refer (refer-pkg)]
   [re-mote.log :refer (setup-logging)]
   re-mote.repl.base)
  (:import [re_mote.repl.base Hosts]))

(refer-timbre)
(refer-facts)
(refer-process)
(refer-base)
(refer-out)
(refer-stats)
(refer-security)
(refer-zero-sensors)
(refer-pkg)
(refer-zero-pkg)
(refer-spec)
(refer-zfs)
(refer-publish)
(refer-octo)
(refer-restic)
(refer-regent)
(refer-git)
(refer-es-persist)
(refer-desktop)
(refer-stress)
(refer-osquery)
(refer-cog)

(defn setup
  "Setup Re-mote environment as a part of the Reload workflow"
  []
  (check-entropy 200)
  (check-jce)
  (setup-logging)
  (setup-stats 10 10))

(defn single
  "Create a single hosts instance"
  [h & m]
  (Hosts. (merge {:user "re-ops"} (first m)) [h]))

; security

(defn ^{:category :security} ports-persist
  "Scan for open ports and persist into ES:
     (ports-persist hs \"192.168.1.0/24\")"
  [hs network]
  (run (open-ports hs "-T5" network) | (enrich "nmap-scan") | (split by-hosts) | (split nested) | (persist)))

(defn ^{:category :security} port-scan
  "Scan for running open ports on the network:
     (port-scan hs \"192.168.1.0/24\")"
  [hs network]
  (run> (open-ports hs "-T5" network) | (pretty "ports scan")))

(defn ^{:category :security} host-scan
  "Scan for running hosts on the network:
     (host-scan hs \"192.168.1.0/24\")"
  [hs network]
  (run> (security/hosts hs "-sP" network) | (pretty "hosts scan")))

(defn ^{:category :security} inactive-firewall
  "Find hosts with inactive firewall:
     (inactive-firewall hs)"
  [hs]
  (run> (rules hs) | (pick (fn [success failure hosts] (mapv :host (failure 1))))))

; persistent stats


(defn ^{:category :stats} du-persist
  "Collect disk usage with persist (metrics collection):
     (du-persist hs)"
  [hs]
  (run> (du hs) | (enrich "du") | (persist) | (riemann)))

(defn ^{:category :stats} cpu-persist
  "Collect CPU and idle usage with persistence (metrics collection):
     (cpu-persist hs)
  "
  [hs]
  (run> (cpu hs) | (enrich "cpu") | (persist) | (riemann)))

(defn ^{:category :stats} entropy-persist
  "Collect Available entropy with persistence (metrics collection):
     (entropy-persist hs)
  "
  [hs]
  (run> (entropy hs) | (enrich "entropy") | (persist) | (riemann)))

(defn ^{:category :stats} ram-persist
  "Collect free and used RAM usage with persistence (metrics collection):
     (ram-persist hs)
  "
  [hs]
  (run> (free hs) | (enrich "free") | (persist) | (riemann)))

(defn ^{:category :stats} net-persist
  "Collect networking in/out kbps and persist (metric collection):
     (net-persist hs)
  "
  [hs]
  (run> (net hs) | (enrich "net") | (persist) | (riemann)))

(defn ^{:category :stats} sensor-persist
  "Collect Sensor data (using lm-sensors) and persist (metric collection):
     (sensor-persist hs)
   "
  [hs]
  (run> (zsens/sensor hs) | (enrich "sensor") | (persist) | (riemann)))

(defn ^{:category :stats} load-persist
  "Read average load and persist is (metrics collection):
     (load-persist hs)
   "
  [hs]
  (run> (load-avg hs) | (enrich "load") | (persist) | (riemann)))

(defn tofrom
  "Email configuration used to send emails"
  [desc]
  {:to "narkisr@gmail.com" :from "gookup@gmail.com" :subject (<< "Running ~{desc} results")})

; Packaging
(defn- update-
  "Update with downgrading"
  [hs]
  (run> (zpkg/update hs) | (downgrade pkg/update) | (pretty "update")))

(defn- upgrade-
  "Run upgrade with downgrading (private)"
  [hs m]
  (run> (zpkg/upgrade hs) | (downgrade pkg/upgrade)))

(defn ^{:category :packaging} update
  "Update the package repository of the hosts:
     (update hs)
  "
  [hs]
  (run (update- hs) | (email (tofrom "package update")) | (enrich "update") | (persist)))

(defn ^{:category :packaging} upgrade
  "Run package update followed by an upgrade on hosts that were updated successfully:
     (upgrade hs)
    "
  [hs]
  (run (update- hs) | (pick successful) | (upgrade-) | (pretty "upgrade") | (email (tofrom "package upgrade")) | (enrich "upgrade") | (persist)))

(defn ^{:category :packaging} install
  "Install a package on hosts:
     (install hs \"openjdk8-jre\")
  "
  [hs pkg]
  (run (zpkg/install hs pkg) | (downgrade pkg/install [pkg]) | (pretty "package install")))

; Re-cog
(defn ^{:category :re-cog} provision
  "Provision hosts copying local file resources and then applying f:
     (provision hs into-hostnames {:src src :f f :args args})

   * into-hostnames - A function that maps result Hosts ips into hostnames
  "
  [hs into-hostnames {:keys [src f args]}]
  {:pre [src f args]}
  (let [dest (<< "/tmp/~(fs/base-name src)")]
    (run> (rm hs dest "-rf") | (sync- src dest) | (pick successful) | (convert into-hostnames) | (run-inlined f args) | (pretty "provision"))))

; Re-gent
(defn ^{:category :re-gent} deploy
  "Deploy re-gent and setup .curve remotely:
     (deploy hs \"re-gent/target/re-gent\")"
  [{:keys [auth] :as hs} bin]
  (let [{:keys [user]} auth home (<< "/home/~{user}") dest (<< "~{home}/.curve")]
    (run (mkdir hs dest "-p") | (scp ".curve/server-public.key" dest) | (pretty "curve copy"))
    (run (kill-agent hs) | (pretty "kill agent"))
    (run> (scp hs bin home) | (pick successful) | (start-agent home) | (pretty "scp"))))

(defn ^{:category :re-gent} kill
  "Kill a re-gent process on all of the hosts:
     (kill hs)"
  [hs]
  (run (kill-agent hs) | (pretty "kill agent")))

(defn ^{:category :re-gent} launch
  "Start a re-gent process on hosts:
     (launch hs)
  "
  [{:keys [auth] :as hs}]
  (let [{:keys [user]} auth home (<< "/home/~{user}")]
    (run (start-agent hs home) | (pretty "launch agent"))))

(defn pull
  "Pull latest git repository changes:
     (pull hs {:repo \"re-core\" :branch \"master\" :remote \"git://github.com/re-ops/re-mote.git\"})"
  [hs {:keys [repo remote branch]}]
  (run (git/pull hs repo remote branch) | (pretty "git pull")))

(defn filter-hosts
  "Filter a sub group out of the provided hosts, the filtering function gets OS information:
     (filter-hosts hs (fn [os] TODO ))"
  [hs f]
  (run (os-info hs) | (pick (partial results-filter f)) | (pretty "filter hosts")))

(defn host-os-info
  "Hosts information using oshi:
    (host-info hs)"
  [hs]
  (run> (os-info hs) | (pretty "filter hosts")))

(defn host-hardware-info
  "Hosts information using oshi:
    (host-info hs)"
  [hs]
  (run> (hardware-info hs) | (pretty "filter hosts")))

; sanity testing
(defn failing
  "A workflow that always fail:
     (failing hs)"
  [hs]
  (run (tst/listdir hs "/") | (pick successful) | (tst/fail) | (pretty "failing")))

(defn ^{:category :shell} listing
  "List directories under / on the remote hosts:
     (listing hs)"
  [hs]
  (run (ls hs "/" "-la") | (pretty "listing")))

; basic tasks
(defn copy-file
  "Copy a local file to hosts"
  [hs src dest]
  (run (scp hs src dest) | (pretty "file opened")))

; desktop
(defn browse-to
  "Open a browser url:
    (browse-to hs \"github.com\")
  "
  [hs url]
  (run (browse hs url) | (pretty "opened browser")))

(defn open-file
  "Open a file using a remote browser:
     (open-file hs \"/home/foo/bar.pdf\")
   "
  [hs src]
  (let [dest (<< "/tmp/~(fs/base-name src)")]
    (run (scp hs src dest) | (browse dest) | (pretty "file opened"))))

; process management

(defn process-matching
  "Find processes matching target name:
    (process-matching hs \"ssh\"); find all ssh processes
  "
  [hs target]
  (run> (processes hs target) | (pretty "process-matching")))
