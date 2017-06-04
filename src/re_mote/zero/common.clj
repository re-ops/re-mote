(ns re-mote.zero.common
  (:import 
     [org.zeromq ZMQ]
     [java.nio.charset Charset])) 

(defn context [] (ZMQ/context 1))

(defonce utf8 (Charset/forName "UTF-8"))

(defn read-key [k]
  (.getBytes (slurp k) utf8))

(defn close! [sockets]
  (doseq [[k s] sockets] (.close s)))
