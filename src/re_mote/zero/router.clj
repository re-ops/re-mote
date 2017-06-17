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
  (.sendMore socket address)
  (.send socket content))

(defn handle-message [socket address content]
   (info content ":" address)
   (reply address content socket))

(defn worker [ctx]
  (let [socket (worker-socket ctx)]
    (try
      (while true
         (info "recieving")
         (let [msg (ZMsg/recvMsg socket) address (.pop msg) content (.pop msg)]
           (info "got a message")
           (assert (not (nil? content)))
           (handle-message socket (.toString address) (.toString content))))
     (catch Exception e
       (error (.getMessage e)))
     (finally
       (.close ctx)))))

(defn setup-server [private n]
  (let [ctx (context) frontend (router-socket ctx private) backend (backend-socket ctx)]
    (reset! sockets {:frontend frontend  :backend backend})
    (dotimes [i n] (future (worker ctx)))
    (ZMQ/proxy frontend backend nil)))

(comment
  (reply "0004-0009" "Oh realy?" (:frontend  @sockets))
  (println @sockets)
  (close! @sockets)
  (future (worker))
  (future (setup-server ".curve/server-private.key")))
