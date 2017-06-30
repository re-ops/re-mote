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

(ns re-mote.zero.common
  (:require 
    [clojure.core.strint :refer  (<<)])
  (:import 
     [org.zeromq ZMQ]
     [java.nio.charset Charset])) 

(defn context [] (ZMQ/context 1))

(defonce utf8 (Charset/forName "UTF-8"))

(defn read-key [k]
  (.getBytes (slurp k) utf8))

(defn close! [sockets]
  (doseq [[k s] sockets] (.close s)))

(defn server-socket 
  ([t private]
   (server-socket (context t private)))
  ([ctx t private]
    (doto
      (.socket ctx t)
      (.setCurveServer true)
      (.setCurveSecretKey (read-key private)))))

(defn client-socket [t parent]
  (doto
    (.socket (context) t)
    (.setZAPDomain (.getBytes "global"))
    (.setCurveServerKey (read-key (<< "~{parent}/server-public.key")))
    (.setCurvePublicKey (read-key (<< "~{parent}/client-public.key")))
    (.setCurveSecretKey (read-key (<< "~{parent}/client-private.key")))))
