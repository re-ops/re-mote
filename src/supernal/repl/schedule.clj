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

(ns supernal.repl.schedule
  "Schedule tasks"
  (:require 
      [clj-time.periodic :refer  [periodic-seq]]
      [taoensso.timbre :refer (refer-timbre)]
      [chime :refer [chime-ch]]
      [clj-time.core :as t]
      [clojure.core.async :as a :refer [<! go-loop close!]]))

(refer-timbre)

(defn create-ch [s] 
  (chime-ch (periodic-seq  (t/now) (-> s t/seconds))))

(defn watch [ch]
 (future
   (a/<!! 
     (go-loop []
       (when-let [msg (<! ch)]
         (info "Chiming at:" msg)
      (recur))))))

(def my-ch (create-ch 1))
;; (close! my-ch)
; (watch my-ch)
