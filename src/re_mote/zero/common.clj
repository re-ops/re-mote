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
