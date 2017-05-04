(comment
   re-mote, Copyright 2012 Ronen Narkis, narkisr.com
   Licensed under the Apache License,
   Version 2.0  (the "License") you may not use this file except in compliance with the License.
   You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.)

(ns re-mote.launch
  (:require
    [taoensso.timbre :refer (refer-timbre)]
    [re-mote.publish.server :as server]
    [re-mote.repl :as repl]
    [re-mote.repl.schedule :as sc]
    [cliopatra.command :as command :refer  [defcommand]])
  (:gen-class true))

(refer-timbre)

(defn setup []
  (repl/setup))

(defn start [_]
  (server/start))

(defn stop [_]
  (sc/halt!)
  (server/stop))

(defn -main [& args])

