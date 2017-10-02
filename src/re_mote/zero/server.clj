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
    (.setZapDomain (.getBytes "global")) ;
    (.bind (str "tcp://*:" port))))

(defn backend-socket [ctx]
  (doto (.socket ctx ZMQ/DEALER)
    (.bind "inproc://backend")))

(defn control-sub-socket [ctx]
  (doto (.socket ctx ZMQ/SUB)
    (.subscribe ZMQ/SUBSCRIPTION_ALL)
    (.connect "inproc://control")))

(defn control-pub-socket [ctx]
  (doto (.socket ctx ZMQ/PUB)
    (.bind "inproc://control")))

(def sockets (atom {}))
(def frontend (agent nil))
(def front-port (atom nil))

(defn send- [address content]
  (send frontend
    (fn [socket]
      (.send socket (freeze address) ZMQ/SNDMORE)
      (.send socket (freeze content) 0)
       socket
      )))

(defn setup-server [ctx private]
  (let [port (find-port 9000 9010)]
    (send frontend (fn [_] (router-socket ctx private port)))
    (info "started zeromq server router socket on port" port)
    (reset! front-port port)
    (reset! sockets {
       :backend (backend-socket ctx) :control-sub (control-sub-socket ctx)
       :control-pub (control-pub-socket ctx)
    })))

(def t (atom nil))

(defn- bind []
  (let [{:keys [backend control-sub]} @sockets]
    (try
      (ZMQ/proxy @frontend backend nil control-sub)
      (finally
        (close! (assoc @sockets :frontend @frontend))
        (send frontend (fn [_] nil))
        (reset! sockets nil)
        (info "proxy closed")))))

(defn bind-future []
  (reset! t (future (bind))))

(defn kill-server! []
  (info "killing server")
  (when @sockets
    (when-let [pub (@sockets :control-pub)]
      (try
        (debug "proxy shutdown call")
        (assert (.send pub "TERMINATE" 0))
        (catch Exception e
          (error (.getMessage e) (.getStacktrace e))))))
  (when @t
    (reset! t nil)))

(comment
  (kill-server!)
  (close! @sockets)
  (reset! sockets {})
  (setup-server (context) ".curve/server-private.key"))
