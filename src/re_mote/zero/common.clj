(ns re-mote.zero.common
  (:require
   [re-share.core :refer (error-m)]
   [clojure.core.strint :refer  (<<)])
  (:import
   [org.zeromq ZMQ]
   [java.nio.charset Charset]))

(defn context [] (ZMQ/context 1))

(defonce utf8 (Charset/forName "UTF-8"))

(defn read-key [k]
  (.getBytes (slurp k) utf8))

(defn close [s]
  (try
    (.setLinger s 0)
    (.close s)
    (catch Exception e
      (error-m e))))

(defn close! [sockets]
  (doseq [[k s] sockets] (close s)))

(defn server-socket
  [ctx t private]
  (doto (.socket ctx t)
    (.setAsServerCurve true)
    (.setCurveSecretKey (read-key private))))

(defn client-socket [t parent]
  (doto
   (.socket (context) t)
    (.setZapDomain (.getBytes "global"))
    (.setCurveServerKey (read-key (<< "~{parent}/server-public.key")))
    (.setCurvePublicKey (read-key (<< "~{parent}/client-public.key")))
    (.setCurveSecretKey (read-key (<< "~{parent}/client-private.key")))))
