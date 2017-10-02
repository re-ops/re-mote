(ns re-mote.zero.worker
  (:require
   [taoensso.timbre :refer  (refer-timbre)]
   [taoensso.nippy :as nippy :refer (freeze thaw)]
   [re-mote.zero.management :refer (process)])
  (:import
   [org.zeromq ZMQ ZMsg]))

(refer-timbre)

(def workers (atom {}))
(def flags (atom {}))

(defn worker-socket [ctx]
  (doto
   (.socket ctx ZMQ/DEALER)
    (.connect "inproc://backend")))

(defn handle-message [socket address content]
  (try
    (let [{:keys [hostname uid] :as m} (thaw address)]
      (debug "got message from" hostname "uid" uid)
      (process m (thaw content)))
    (catch Exception e
      (error e (.getMessage e)))))

(defn worker [ctx i]
  (let [socket (worker-socket ctx)]
    (try
      (info "worker running")
      (while (@flags i)
        (when-let [msg (ZMsg/recvMsg socket)]
          (let [address (.pop msg) content (.pop msg)]
            (handle-message socket (.getData address) (.getData content)))))
      (info "worker going down")
      (catch Exception e
        (error e (.getMessage e) (.getStacktrace e)))
      (finally
        (.setLinger socket 0)
        (.close socket)
        (info "closed worker socket")))))

(defn setup-workers [ctx n]
  (reset! workers
          (into {}
                (map
                  (fn [i] (swap! flags assoc i true) [i (future (worker ctx i))]) (range n)))))

(defn stop-workers! []
  (info "stopping worker")
  (doseq [[i w] @workers]
    (swap! flags assoc i false))
  (reset! flags {})
  (reset! workers {}))
