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

(defn scrub [hs ds]
  (run (exec hs (<< "/sbin/zpool scrub ~{ds}")) | (pretty)))

(def errors "'(DEGRADED|FAULTED|OFFLINE|UNAVAIL|REMOVED|FAIL|DESTROYED|corrupt|cannot|unrecover)'")

(defn healty [pool errors]
  (script
    (pipe ("/sbin/zpool" "status" ~pool) ("egrep" "-v" "-i" ~errors))))

(defn health [hs pool]
   (run (exec hs (healty pool errors)) | (pretty)))

(defn snapshot [hs pool dataset]
  (let [date (f/unparse (f/formatter "dd-MM-YYYY_hh:mm:ss_SS") (local-now))]
    (run (exec hs (<< "/sbin/zfs snapshot ~{pool}/~{dataset}@~{date}")) | (pretty))))

(defn refer-zfs []
  (require '[re-mote.repl.zfs :as zfs :refer (health snapshot)]))
