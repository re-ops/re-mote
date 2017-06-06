(ns re_mote.zero.router
  "Router socket"
  (:require
     [clojure.core.strint :refer  (<<)]
     [taoensso.timbre :refer  (refer-timbre)]
     [re-mote.zero.common :refer  (read-key server-socket context close!)])
  (:import
     [org.zeromq ZMsg ZMQ ZMQ$PollItem ZMQ$Poller]
     [java.nio.charset Charset]))

(refer-timbre)

(defn router-socket [ctx private]
  (doto (server-socket ctx ZMQ/ROUTER private)
    (.setZAPDomain (.getBytes "global")) ;
    (.bind "tcp://*:9000")))

(defn backend-socket [ctx]
  (doto (.socket ctx ZMQ/DEALER)
    (.bind "inproc://backend")))

(defn worker-socket [ctx]
  (doto (.socket ctx ZMQ/DEALER)
    (.connect "inproc://backend")))

(def sockets (atom {}))

(defn setup-server [private]
  (let [ctx (context) frontend (router-socket ctx private) backend (backend-socket ctx)]
    (reset! sockets {:frontend frontend  :backend backend})
    (ZMQ/proxy frontend backend nil)
    ))

(defn worker []
  (let [ctx (context)]
    
    ))

(comment 
  (close! @sockets)
  (future (setup-server ".curve/server-private.key")))
