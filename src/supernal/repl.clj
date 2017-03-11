(comment
  Celestial, Copyright 2017 Ronen Narkis, narkisr.com
  Licensed under the Apache License,
  Version 2.0  (the "License") you may not use this file except in compliance with the License.
  You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.)

(ns supernal.repl
  "Repl utilities for supernal"
  (:require
    [supernal.validate :refer (check-entropy check-jce)]
    [clojure.pprint :refer (pprint)]
    [taoensso.timbre :refer (refer-timbre set-level!)]
    [supernal.repl.base :refer (refer-base)]
    [supernal.repl.output :refer (refer-out setup-logging)]
    [supernal.repl.pkg :refer (refer-pkg)]
    [supernal.log :refer (run-purge)]
    [supernal.repl.stats :refer (refer-stats)])
  (:import [supernal.repl.base Hosts]))

(refer-base)
(refer-out)
(refer-stats)
(refer-pkg)

(defn setup []
  (check-entropy 200)
  (check-jce)
  (setup-logging)
  (run-purge 10))

(setup)

(def sandbox (Hosts. {:user "vagrant"} ["192.168.2.25" "192.168.2.26" "192.168.2.27" "192.168.2.28"]))

(def local (into-hosts "local.edn"))

(defn listing [hs]
  (run (ls hs "/" "-la") | (pretty)))

(defn stats [hs]
  (run (free hs) | (pretty))
  (run (cpu hs) | (pretty)))

(defn update-n-upgrade [hs]
  (run (update hs) | (pretty) | (pick successful) | (upgrade) | (pretty)))
