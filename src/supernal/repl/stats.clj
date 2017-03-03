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

(ns supernal.repl.stats
  (:require 
    [supernal.repl.base :refer (run-hosts)]
    [pallet.stevedore :refer (script)])
  (:import [supernal.repl.base Hosts]))

(defprotocol Cpu
  (idle [this hosts]))

(extend-type Hosts
  Cpu
  (idle [this {:keys [hosts]}]
     (let [stat (script (pipe ("mpstat" "2" "1") ("awk" "'{ print $12 }'") ("tail" "-1")))]
       [this (run-hosts (:auth this) hosts stat)])))
