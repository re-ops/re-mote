(ns re-mote.zero.pub
  "publish to agents"
  (:require
    [taoensso.timbre :refer (refer-timbre)]
   [re-mote.zero.common :refer (read-key context close!)])
  (:import
    [org.zeromq ZMQ]
    [java.nio.charset Charset]))

(refer-timbre)

(defn server-socket [t private]
   [t private]
  (doto
    (.socket (context) t)
    (.setCurveServer true)
    (.setCurveSecretKey (read-key private))))

(defn pub-socket [private]
  (doto (server-socket ZMQ/PUB private)
    (.setZAPDomain (.getBytes "global")) ;
    (.bind "tcp://*:9000")))

(defn rep-socket [private]
  (doto (server-socket ZMQ/REP private)
    (.bind "tcp://*:9001")))

(def sockets (atom {}))

(defn setup-server [private]
  (reset! sockets
    {:rep (rep-socket private) :pub (pub-socket private)}))

(defn publish [msg topic]
  (let [{:keys [pub rep]} @sockets]
    (.sendMore pub topic)
    (.send pub msg)
    (info (.recvStr rep 0 (Charset/defaultCharset)))
    ))

(comment
  (setup-server ".curve/server-private.key")
  (close! @sockets)
  (future (publish "/bin/ls /" "play")))
