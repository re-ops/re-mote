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

(defn client-socket [t parent]
  (doto
    (.socket (context) t)
    (.setCurveServerKey (read-key (<< "~{parent}/server-public.key")))
    (.setCurvePublicKey (read-key (<< "~{parent}/client-public.key")))
    (.setCurveSecretKey (read-key (<< "~{parent}/client-private.key")))
    ))

(defn sub-socket [host parent]
  (doto (client-socket ZMQ/SUB parent)
    (.connect (<< "tcp://~{host}:9000"))))

(defn req-socket [host parent]
  (doto (client-socket ZMQ/REQ parent)
    (.connect (<< "tcp://~{host}:9001"))))

(def sockets (atom {}))

(defn setup-client [host private]
  (reset! sockets
    {:req (req-socket host private) :sub (sub-socket host private)}))

(defn read-loop [topic]
   (let [{:keys [sub req]} @sockets]
      (.subscribe sub (.getBytes topic));
      (loop []
        (info "waiting")
        (info (.recvStr sub 0 (Charset/defaultCharset)))
        (.send req "ok")
        (recur))))

(comment
  (setup-client "127.0.0.1" ".curve")
  (future (read-loop "play"))
  )
