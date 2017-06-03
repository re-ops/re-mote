(ns re-mote.zero.pub
  "publish to agents"
  (:require
   [re-mote.zero.common :refer (read-key context)])
  (:import 
    [org.zeromq ZMQ]
    [java.nio.charset Charset])
  )


(defn pub-socket [private]
  (doto
    (.socket (context) ZMQ/PUB)
    (.setCurveServer true)
    (.setCurveSecretKey (read-key private))
    (.setZAPDomain (.getBytes "global")) ;
    (.bind "tcp://*:9000")
    ))

(declare socket)

(defn setup [private]
  (def socket (atom (pub-socket))))

(defn publish [msg topic]
   (.sendMore @socket topic)
   (.send @socket msg))

(comment 
  (publish "/bin/ls /" "play"))
