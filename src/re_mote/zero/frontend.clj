(ns re-mote.zero.frontend
  "frontend socket loop"
  (:require
   [re-share.core :refer (error-m)]
   [re-share.zero.common :refer (close)]
   [taoensso.nippy :as nippy :refer (freeze)]
   [re-share.core :refer (find-port)]
   [taoensso.timbre :refer  (refer-timbre)]
   [re-mote.zero.common :refer  (server-socket)])
  (:import [org.zeromq ZMQ]))

(refer-timbre)

(def send-queue (atom (clojure.lang.PersistentQueue/EMPTY)))

(def front-port (atom nil))

(def front-flag (atom nil))

(def front-socket (atom nil))

(defn router-socket [ctx private port]
  (doto (server-socket ctx ZMQ/ROUTER private)
    (.setZapDomain (.getBytes "global")) ;
    (.bind (str "tcp://*:" port))))

(defn front-loop [frontend]
  (info "front loop running")
  (while @front-flag
    (if-let [[address content] (peek @send-queue)]
      (try
        (.send frontend (freeze address) ZMQ/SNDMORE)
        (.send frontend (freeze content) 0)
        (swap! send-queue pop)
        (catch Exception e
          (error-m e)))
      (Thread/sleep 100)))
  (info "front loop done"))

(defn setup-front [ctx private]
  (let [port (find-port 9000 9010)]
    (reset! front-socket (router-socket ctx private port))
    (reset! front-port port)
    (info "started zeromq server router socket on port" port))
  (reset! front-flag true)
  (reset! send-queue (clojure.lang.PersistentQueue/EMPTY))
  (future (front-loop @front-socket))
  @front-socket)

(defn stop-front! []
  (info "killing front loop")
  (reset! front-port nil)
  (reset! front-flag nil)
  (reset! send-queue nil)
  (debug "pre socket close")
  (when @front-socket
    (close @front-socket)
    (reset! front-socket nil)
    (info "front socket closed")))

(defn send- [address content]
  (when @send-queue
    (swap! send-queue conj [address content])))

(defn used-port [] @front-port)
