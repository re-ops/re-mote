(ns re-mote.zero.sub
 (:require
   [clojure.core.strint :refer  (<<)]
   [taoensso.timbre :refer (refer-timbre)]
   [re-mote.zero.common :refer (read-key context)])
 (:import
    [java.nio.charset Charset]
    [org.zeromq ZMQ]
    [java.nio.charset Charset]))

(refer-timbre)

(defn sub-socket [host parent]
  (doto
    (.socket (context) ZMQ/SUB)
    (.setCurveServerKey (read-key (<< "~{parent}/server-public.key")))
    (.setCurvePublicKey (read-key (<< "~{parent}/client-public.key")))
    (.setCurveSecretKey (read-key (<< "~{parent}/client-private.key")))
    (.connect (<< "tcp://~{host}:9000"))))

(defn read-loop [topic]
   (let [client (sub-socket)]
      (.subscribe client (.getBytes topic));
      (loop []
        (debug "waiting")
        (debug (.recvStr client 0 (Charset/defaultCharset)))
        (recur))))

;; (read-loop "play")
