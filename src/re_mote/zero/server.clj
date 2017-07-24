(ns re-mote.zero.server
  "An orchestration re-mote server using Zeromq router socket"
  (:require
   [re-share.core :refer (find-port)]
   [taoensso.nippy :as nippy :refer (freeze)]
   [clojure.core.strint :refer  (<<)]
   [taoensso.timbre :refer  (refer-timbre)]
   [re-mote.zero.common :refer  (read-key server-socket context close!)])
  (:import [org.zeromq ZMQ]))

(refer-timbre)

(defn router-socket [ctx private port]
  (doto (server-socket ctx ZMQ/ROUTER private)
    (.setZAPDomain (.getBytes "global")) ;
    (.bind (str "tcp://*:" port))))

(defn backend-socket [ctx]
  (doto (.socket ctx ZMQ/DEALER)
    (.bind "inproc://backend")))

(def sockets (atom {}))
(def front-port (atom nil))

(defn reply [address content]
  (let [{:keys [frontend]} @sockets]
    (.send frontend (freeze address) ZMQ/SNDMORE)
    (.send frontend (freeze content) 0)))

(defn setup-server [ctx private]
  (let [port (find-port 9000 9010)
        frontend (router-socket ctx private port)
        backend (backend-socket ctx)]
    (info "started zeromq server router socket on port" port)
    (reset! front-port port)
    (reset! sockets {:frontend frontend :backend backend})))

(defn bind []
  (let [{:keys [frontend backend]} @sockets]
    (ZMQ/proxy frontend backend nil)))

(defn kill-server! []
  (close! @sockets))

(comment
  (kill-server!)
  (future (setup-server ".curve/server-private.key" 4)))
