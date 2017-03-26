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

(ns re-mote.repl.publish
  (:require
    [com.rpl.specter :as s :refer (transform select MAP-VALS ALL ATOM keypath srange)]
    [clojure.pprint :refer (pprint)]
    [taoensso.timbre :refer (refer-timbre)]
    [re-mote.publish.server :refer (broadcast!)]
    [re-mote.repl.stats :refer (single-per-host avg-all)]
    [re-mote.repl.stats :refer (readings)]
    [re-mote.repl.base :refer (refer-base)])
  (:import [re_mote.repl.base Hosts]))

(defprotocol Publishing
  (publish [this m spec])
  (email [this m]))

(defn stock [n k & ks]
  {:graph {:gtype :vega/stock :gname n} :values-fn (partial single-per-host k ks)})

(defn lines [n]
  {:graph {:gtype :vega/lines :gname n} :values-fn identity})

(defn stack [n k]
  {:graph {:gtype :vega/stack :gname n} :values-fn (partial avg-all k)})

(extend-type Hosts
   Publishing
   (publish [this {:keys [success] :as m} {:keys [graph values-fn]}]
      (broadcast! [::vega {:values (sort-by :x (values-fn success)) :graph graph}])
      [this m]
     ))

(defn refer-publish []
  (require '[re-mote.repl.publish :as pub :refer (publish stock stack lines)])
  )

