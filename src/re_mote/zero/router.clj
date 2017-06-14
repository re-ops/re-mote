(ns re_mote.zero.router
  "Router socket"
  (:require
     [clojure.core.strint :refer  (<<)]
     [taoensso.timbre :refer  (refer-timbre)]
     [re-mote.zero.common :refer  (read-key server-socket context close!)])
  (:import
     [org.zeromq ZMsg ZMQ ZMQ$PollItem ZMQ$Poller ZFrame]
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

(defn reply [address content socket]
  (try
    (.send address socket ZMQ/SNDMORE)
    (.send content socket ZMQ/SNDMORE)
    (finally
      (.destroy address)
      (.destroy content))))

(defn worker []
  (let [ctx (context) socket (worker-socket ctx)]
    (try
      (.connect socket)
      (while true
         (let [msg (ZMsg/recvMsg socket) address (.pop msg) content (.pop msg)]
           (info "recieving")
           (assert (not (nil? content)))
           (.destroy msg)
           (reply address content socket)))
     (catch Exception e
       (error e))
     (finally
       (.close ctx)))))

(defn setup-server [private]
  (let [ctx (context) frontend (router-socket ctx private) backend (backend-socket ctx)]
    (reset! sockets {:frontend frontend  :backend backend})
    (ZMQ/proxy frontend backend nil)))

(comment
  (println @sockets)
  (close! @sockets)
  (future (worker))
  (future (setup-server ".curve/server-private.key")))
