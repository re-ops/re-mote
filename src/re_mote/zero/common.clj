(ns re-mote.zero.common
  (:require
   [taoensso.nippy :as nippy :refer (freeze)]
   [taoensso.timbre :refer  (refer-timbre)]
   [re-share.core :refer (error-m)]
   [clojure.core.strint :refer  (<<)])
  (:import
   [org.zeromq ZMQ]
   [java.nio.charset Charset]))

(refer-timbre)

(defonce utf8 (Charset/forName "UTF-8"))

(defn read-key [k]
  (.getBytes (slurp k) utf8))

(defn server-socket
  [ctx t private]
  (doto (.socket ctx t)
    (.setLinger 0)
    (.setAsServerCurve true)
    (.setCurveSecretKey (read-key private))))

(defn send-
  ([socket content]
   (try
     (.send socket (freeze content) 0)
     (catch Exception e
       (error-m e))))
  ([socket address content]
   (debug "sending" address content)
   (try
     (.send socket (freeze address) ZMQ/SNDMORE)
     (.send socket (freeze content) 0)
     (catch Exception e
       (error-m e)))))
