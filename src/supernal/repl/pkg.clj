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

(ns supernal.repl.pkg
  "Package automation"
   (:require
     [clojure.string :refer (split join)]
     [supernal.sshj :refer (get-log)]
     [supernal.repl.base :refer (run-hosts)]
     [pallet.stevedore :refer (script)])
   (:import [supernal.repl.base Hosts]))


(defprotocol Apt
  (update [this]) 
  (upgrade [this])
  )

(extend-type Hosts
   Apt
   (update [this]
     [this (run-hosts this (script ("sudo" "apt" "update")))])  

   (upgrade [this]
     [this (run-hosts this (script ("sudo" "apt" "upgrade" "-y")))]))

(defn refer-pkg []
  (require '[supernal.repl.pkg :as pkg :refer (update upgrade)]))
