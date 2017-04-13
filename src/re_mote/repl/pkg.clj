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

(ns re-mote.repl.pkg
  "Package automation"
   (:refer-clojure :exclude  [update])
   (:require
     [clojure.string :refer (split join)]
     [re-mote.log :refer (get-log)]
     [re-mote.repl.base :refer (run-hosts)]
     [pallet.stevedore :refer (script)])
   (:import [re_mote.repl.base Hosts]))


(defprotocol Apt
  (update
    [this]
    [this m])
  (upgrade [this m])
  (install 
    [this pkg]
    [this m pkg]))

(extend-type Hosts
   Apt
   (update [this _]
     (update this))

   (update [this]
     [this (run-hosts this (script ("sudo" "apt" "update")))])

   (upgrade [this _]
     [this (run-hosts this (script ("sudo" "apt" "upgrade" "-y")))])

   (install [this _ pkg]
      (install this pkg))

   (install [this pkg]
     [this (run-hosts this (script ("sudo" "apt" "install" ~pkg "-y")))]))

(defn refer-pkg []
  (require '[re-mote.repl.pkg :as pkg :refer (update upgrade install)]))
