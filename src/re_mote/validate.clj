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

(ns re-mote.validate
 (:require
  [clojure.string :refer (trim)])
 (:import
   [javax.crypto Cipher])
)

(defn- read-entropy []
  (Integer/parseInt (trim (slurp "/proc/sys/kernel/random/entropy_avail"))))

(defn check-entropy [limit]
  (let [entropy (read-entropy)]
    (when (and (= "Linux" (System/getProperty "os.name")) (< entropy limit))
      (throw (ex-info "Available entropy is too low" {:available entropy :limit limit})))))

(defn check-jce []
  (let [allowed (Cipher/getMaxAllowedKeyLength "AES")]
    (when (> Integer/MAX_VALUE allowed)
      (throw (ex-info "JCE isn't available" {:max-allowed allowed})))))

