(ns re-mote.zero.server
  "An orchestration re-mote server using ZeroMq router socket"
  (:require
   [clojure.core.strint :refer  (<<)]
   [taoensso.timbre :refer  (refer-timbre)]
   [re-share.core :refer (error-m)]
   [re-share.zero.common :refer (context close!)]
   [re-mote.zero.common :refer (read-key server-socket)])
  (:import [org.zeromq ZMQ]))

(refer-timbre)

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

(defn setup-server [ctx private]
  (reset! sockets {:backend (backend-socket ctx)
                   :control-sub (control-sub-socket ctx)
                   :control-pub (control-pub-socket ctx)}))

(defn- bind [frontend]
  (let [{:keys [backend control-sub]} @sockets]
    (try
      (ZMQ/proxy frontend backend nil control-sub)
      (finally
        (close! @sockets)
        (reset! sockets nil)
        (info "proxy closed")))))

(defn bind-future [frontend]
  (future (bind frontend)))

(defn kill-server! []
  (info "killing server")
  (when @sockets
    (when-let [pub (@sockets :control-pub)]
      (try
        (debug "proxy shutdown call")
        (assert (.send pub "TERMINATE" 0))
        (catch Exception e
          (error-m e))))))
