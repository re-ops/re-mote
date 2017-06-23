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

(ns re-mote.repl.re-gent
  "Copy .curve server public key and run agent remotly"
  (:require 
    [re-mote.repl.base :refer (run-hosts)])
  (:import [re_mote.repl.base Hosts]))

(defprotocol Regent
  (launch [this])
  )

(extend-type Hosts
  Regent
  )

(defn refer-regent []
  (require '[re-mote.repl.re-gent :as re-gent :refer (launch)]))
