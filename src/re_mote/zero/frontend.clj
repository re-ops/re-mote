(ns re-mote.zero.frontend
  "frontend socket loop"
  (:require
    [re-mote.zero.common :refer (close)]
    [taoensso.nippy :as nippy :refer (freeze)]
    [re-share.core :refer (find-port)]
    [taoensso.timbre :refer  (refer-timbre)]
    [re-mote.zero.common :refer  (server-socket)])
  (:import [org.zeromq ZMQ]))

(refer-timbre)

(def send-queue (agent (clojure.lang.PersistentQueue/EMPTY)))

(def front-port (atom nil))

(def front-flag (atom nil))

(def front-socket (atom nil))

(defn router-socket [ctx private port]
  (doto (server-socket ctx ZMQ/ROUTER private)
    (.setZapDomain (.getBytes "global")) ;
    (.bind (str "tcp://*:" port))))

(defn front-loop [frontend]
  (info "front loop running")
  (try 
    (while @front-flag
      (if-let [[address content] (peek @send-queue)]
        (do 
          (.send frontend (freeze address) ZMQ/SNDMORE)
          (.send frontend (freeze content) 0)
          (send send-queue pop))
        (Thread/sleep 100)))
    (finally
      (close @front-socket) 
      (reset! front-socket nil)))
  (info "front loop done"))

(defn setup-front [ctx private]
  (let [port (find-port 9000 9010)]
    (reset! front-socket (router-socket ctx private port))
    (reset! front-port port)
    (info "started zeromq server router socket on port" port))
  (reset! front-flag true)
  (send send-queue (fn [_] (clojure.lang.PersistentQueue/EMPTY)))
  (future (front-loop @front-socket))
  @front-socket)

(defn stop-front! []
  (info "killing front loop")
  (reset! front-port nil)
  (reset! front-flag nil)
  (send send-queue (fn [_] nil)))

(defn send- [address content]
  (send send-queue conj [address content]))

(defn used-port []
  @front-port
  )
