(ns re-mote.zero.common
  (:require
   [re-share.core :refer (error-m)]
   [clojure.core.strint :refer  (<<)])
  (:import
   [org.zeromq ZMQ]
   [java.nio.charset Charset]))

(defonce utf8 (Charset/forName "UTF-8"))

(defn read-key [k]
  (.getBytes (slurp k) utf8))

(defn server-socket
  [ctx t private]
  (doto (.socket ctx t)
    (.setAsServerCurve true)
    (.setCurveSecretKey (read-key private))))

