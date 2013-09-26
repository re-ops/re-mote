(comment 
   Celestial, Copyright 2012 Ronen Narkis, narkisr.com
   Licensed under the Apache License,
   Version 2.0  (the "License") you may not use this file except in compliance with the License.
   You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.)

(ns supernal.baseline
  "A group of base recipes for deployment etc.."
  (:import java.util.Date)
  (:use 
    [clojure.string :only (split)]
    [clojure.core.strint :only (<<)]
    [taoensso.timbre :only (warn debug)]
    [supernal.core :only (ns- lifecycle copy run)]))

(def run-ids (atom {}))

(defn date-fmt []
  (.format (java.text.SimpleDateFormat. "HH-mm-ss_yyyy-MM-dd") (Date.)))

(defn releases [name* id] (<< "/u/apps/~{name*}/releases/~(@run-ids id)/"))

(defn current [name*] (<< "/u/apps/~{name*}/current"))

(defn archive-types [src dst] 
  {"zip" (<< "unzip ~{src} -d ~{dst}") "gz" (<< "tar -xzf ~{src} -C ~{dst}")})

(defn archive? [file]
   ((into #{} (keys (archive-types nil nil))) (last (split file #"\."))))

(ns- deploy 
  (task update-code :desc "updates deployed code"
    (let [{:keys [src app-name run-id]} args]
      (debug "updating code on" remote) 
      (copy src (releases app-name run-id)))) 
 
  (task post-update :desc "runs post code update actions"
    (let [{:keys [src app-name run-id]} args file (last (split src #"/")) 
          basepath (releases app-name run-id)]
      (when-let [ext (archive? file)]
        (debug ext)
        (run ((archive-types (<< "~{basepath}~{file}") basepath) ext)))))

  (task start :desc "starts deployed service"
    (debug "starting service on" remote)) 
 
  (task symlink :desc "links current version to current"
    (let [{:keys [app-name run-id]} args]
      (run (<< "rm -f ~(current app-name)"))
      (run (<< "ln -s ~(releases app-name run-id) ~(current app-name)"))))

  (task stop :desc "stops deployed service"
    (debug "stopping service on" remote))
     
  (task pre-update :desc "pre code update actions"
    (let [{:keys [app-name run-id]} args release-id (date-fmt)]
      (swap! run-ids assoc run-id release-id)
      (run (<< "mkdir ~(releases app-name run-id) -p"))
      (run (<< "chown ~(remote :user) ~(releases app-name run-id)"))))) 


(lifecycle base-rollback {:doc "base deployment roleback"} {})

(lifecycle base-success {:doc "base deployment success"} {})

(lifecycle base-deploy {:doc "base deployment scheme" :failure base-success :success base-rollback}
  {deploy/update-code #{deploy/post-update deploy/symlink}
   deploy/stop #{deploy/update-code}
   deploy/pre-update #{deploy/update-code}
   deploy/symlink #{deploy/start} 
   deploy/post-update #{deploy/start}
   deploy/start #{}})

