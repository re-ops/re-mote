(ns re-mote.zero.server
  "An orchestration re-mote server using ZeroMq router socket"
  (:require
   [re-share.core :refer  (find-port)]
   [clojure.core.strint :refer  (<<)]
   [taoensso.timbre :refer  (refer-timbre)]
   [re-share.core :refer (error-m)]
   [re-share.zero.common :refer (context close)]
   [re-mote.zero.common :refer (read-key server-socket)])
  (:import [org.zeromq ZMQ]))

(refer-timbre)

(defn router-socket [ctx private port]
  (doto (server-socket ctx ZMQ/ROUTER private)
    (.setLinger 0)
    (.setZapDomain (.getBytes "global")) ;
    (.bind (str "tcp://*:" port))))

(defn backend-socket [ctx]
  (doto (.socket ctx ZMQ/DEALER)
    (.setLinger 0)
    (.bind "inproc://backend")))

(defn control-sub-socket [ctx]
  (doto (.socket ctx ZMQ/SUB)
    (.setLinger 0)
    (.subscribe ZMQ/SUBSCRIPTION_ALL)
    (.connect "inproc://control")))

(defn control-pub-socket [ctx]
  (doto (.socket ctx ZMQ/PUB)
    (.setLinger 0)
    (.bind "inproc://control")))

(def front-port (atom nil))

(defn start [ctx private]
  (future
    (let [port 9000
          backend (backend-socket ctx)
          frontend (router-socket ctx private port)
          control (control-sub-socket ctx)]
      (reset! front-port port)
      (info "started zeromq on" port)
      (.monitor frontend "inproc://events" ZMQ/EVENT_ALL)
      (ZMQ/proxy frontend backend nil control)
      (close frontend)
      (close backend)
      (close control)
      (info "stopped zeromq"))))

(defn stop [ctx]
  (let [control (control-pub-socket ctx)]
    (assert (.send control "TERMINATE" 0))
    (close control)
    (debug "server proxy shutdown called")
    (reset! front-port nil)))

(defn used-port
  {:post [(comp not nil?)]}
  []
  @front-port)
