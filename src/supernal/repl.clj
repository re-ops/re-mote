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
    [taoensso.timbre :refer (refer-timbre)]
    [supernal.repl.base :refer (refer-base)]
    [supernal.repl.output :refer (refer-out)]
    [supernal.repl.pkg :refer (refer-pkg)]
    [supernal.repl.publish :refer (refer-publish)]
    [supernal.repl.schedule :refer (watch)]
    [supernal.log :refer (setup-logging)]
    [supernal.repl.stats :refer (refer-stats)])
  (:import [supernal.repl.base Hosts]))

(refer-timbre)
(refer-base)
(refer-out)
(refer-stats)
(refer-pkg)
(refer-publish)

(defn setup []
  (check-entropy 200)
  (check-jce)
  (setup-logging :stream false)
  (setup-stats 10 10)
  )

(def sandbox (Hosts. {:user "vagrant"} ["192.168.2.25" "192.168.2.26" "192.168.2.27"]))

;; (def local (into-hosts "local.edn"))

(defn listing [hs]
  (run (ls hs "/" "-la") | (pretty)))

(defn stats [hs]
  (run (free hs) | (pretty) | (collect) | (publish (stock "Free RAM" :timeseries :free)) | (publish (stock "Used RAM" :timeseries :used)))
  (run (cpu hs)  | (pretty) | (collect) | (publish (stock "Idle CPU" :timeseries :idle)) | (publish (stock "User CPU" :timeseries :usr))))

(defn inlined-stats [hs]
  (run (free hs) | (collect) | (cpu) | (collect) | (sliding avg :avg) | (publish (stock "User cpu avg" :avg :usr))))

(defn aptgrade [hs]
  (run (update hs) | (pretty) | (pick successful) | (upgrade) | (pretty)))

(def ch (atom nil))

(defn stop-periodical-stats []
  (clojure.core.async/close! @ch))

(defn periodical-stats [hs]
  (reset! ch (watch 10 (fn [] (stats hs)))))
