(comment
  re-mote, Copyright 2017 Ronen Narkis, narkisr.com
  Licensed under the Apache License,
  Version 2.0  (the "License") you may not use this file except in compliance with the License.
  You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.)

(ns re-mote.repl
  "Repl utilities for re-mote"
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
    [re-mote.repl.schedule :refer (watch seconds)]
    [re-mote.log :refer (setup-logging)]
    [clojure.java.io :refer (file)]
    [re-mote.repl.stats :refer (refer-stats)])
  (:import [re_mote.repl.base Hosts]))

(refer-timbre)
(refer-base)
(refer-out)
(refer-stats)
(refer-pkg)
(refer-puppet)
(refer-zfs)
(refer-publish)
(refer-octo)

(defn setup []
  (check-entropy 200)
  (check-jce)
  (setup-logging)
  (setup-stats 10 10))

(def sandbox (Hosts. {:user "vagrant"} ["192.168.2.28" "192.168.2.26" "192.168.2.27"]))

(defn listing [hs]
  (run (ls hs "/" "-la") | (pretty)))

(defn stats [hs]
  (run (free hs) | (pretty) | (collect) | (publish (stock "Free RAM" :timeseries :free)) | (publish (stock "Used RAM" :timeseries :used)))
  (run (cpu hs)  | (pretty) | (collect) | (publish (stock "Idle CPU" :timeseries :idle)) | (publish (stock "User CPU" :timeseries :usr))))

(defn inlined-stats [hs]
  (run (free hs) | (collect) | (cpu) | (collect) | (sliding avg :avg) | (publish (stock "User cpu avg" :avg :usr))))

(defn tofrom [desc] 
  {:to "narkisr@gmail.com" :from "gookup@gmail.com" :subject (<< "Running ~{desc} results")})

(defn aptgrade [hs]
  (run (update hs) | (pretty) | (pick successful) | (upgrade) | (pretty) | (email (tofrom "aptgrade"))))

(defn aptdate [hs]
  (run (update hs) | (pretty) | (email (tofrom "aptdate"))))

(defn add-package [hs pkg]
  (run (install hs pkg) | (pretty)))

(defn fail! [hs]
  (run (exec hs "/bin/not-exists") | (email (tofrom "failing!"))))

(defn periodical-stats [hs]
  (watch :stats (seconds 10) (fn [] (stats hs))))

(defn copy-module [hs pkg]
  (let [name (.getName (file pkg))]
    (run (scp hs pkg "/tmp") | (pretty) | (pick successful)
                             | (extract (<< "/tmp/~{name}") "/tmp") | (rm (<< "/tmp/~{name}") "-rf"))))

(defn run-module [hs pkg args]
  (let [[this _] (copy-module hs pkg)  extracted (.replace (.getName (file pkg)) ".tar.gz" "")]
    (run (apply-module this (<< "/tmp/~{extracted}") args) | (pretty) | (rm (<< "/tmp/~{extracted}") "-rf"))))
