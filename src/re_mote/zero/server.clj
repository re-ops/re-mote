(ns re-mote.zero.server
  "An orchestration re-mote server using Zeromq router socket"
  (:require
     [taoensso.nippy :as nippy :refer (freeze)]
     [clojure.core.strint :refer  (<<)]
     [taoensso.timbre :refer  (refer-timbre)]
     [re-mote.zero.common :refer  (read-key server-socket context close!)])
  (:import [org.zeromq ZMQ])
  )

(refer-timbre)

(defn router-socket [ctx private]
  (doto (server-socket ctx ZMQ/ROUTER private)
    (.setZAPDomain (.getBytes "global")) ;
    (.bind "tcp://*:9000")))

(defn backend-socket [ctx]
  (doto (.socket ctx ZMQ/DEALER)
    (.bind "inproc://backend")))

(def sockets (atom {}))

(defn reply [address content]
  (let [{:keys [frontend]} @sockets]
    (.send frontend (freeze address) ZMQ/SNDMORE)
    (.send frontend (freeze content) 0)))

(defn setup-server [ctx private]
  (let [frontend (router-socket ctx private) backend (backend-socket ctx)]
    (reset! sockets {:frontend frontend  :backend backend})))

(defn bind []
  (let [{:keys [frontend backend]} @sockets]
    (ZMQ/proxy frontend backend nil)))

(defn kill-server! []
   (close! @sockets))

(comment
  (kill-server!)
  (future (setup-server ".curve/server-private.key" 4)))
