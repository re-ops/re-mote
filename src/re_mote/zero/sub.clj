(ns re-mote.zero.sub
 (:require
   [clojure.core.strint :refer  (<<)]
   [taoensso.timbre :refer (refer-timbre)]
   [re-mote.zero.common :refer (read-key client-socket context)])
 (:import
    [java.nio.charset Charset]
    [org.zeromq ZMQ]
    [java.nio.charset Charset]))

(refer-timbre)

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
