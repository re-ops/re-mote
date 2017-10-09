(ns re-mote.zero.worker
  (:require
   [taoensso.timbre :refer  (refer-timbre)]
   [taoensso.nippy :as nippy :refer (freeze thaw)]
   [re-mote.zero.common :refer (send-)]
   [re-share.zero.common :refer (close)]
   [re-share.core :refer (error-m)]
   [re-mote.zero.management :refer (process)])
  (:import
   [org.zeromq ZMQ ZMsg]))

(refer-timbre)

(def workers (atom {}))

(def flags (atom {}))

(defn worker-socket [ctx]
  (doto (.socket ctx ZMQ/DEALER)
    (.setLinger 0)
    (.connect "inproc://backend")))

(defn send-socket [ctx]
  (doto (.socket ctx ZMQ/PULL)
    (.setLinger 0)
    (.connect "inproc://send-out")))

(defn unpack [msg]
  (-> msg (.pop) (.getData) thaw))

(defn handle-incomming [socket]
  (try
    (when-let [msg (ZMsg/recvMsg socket ZMQ/DONTWAIT)]
      (let [address (unpack msg) content (unpack msg)
            {:keys [hostname uid] :as m} address]
        (debug "got message from" hostname "uid" uid)
        (when-let [reply (:reply (process m content))]
          (send- socket address reply))
        true))
    (catch Exception e
      (error-m e))))

(defn handle-sends [in out]
  (when-let [msg (ZMsg/recvMsg in ZMQ/DONTWAIT)]
    (let [{:keys [address content]} (unpack msg)]
      (info address content)
      (send- out address content)
      true)))

(defn worker [ctx i]
  (let [ws (worker-socket ctx) snd (send-socket ctx)]
    (try
      (info "worker running")
      (while (@flags i)
        (let [incoming (handle-incomming ws) sends (handle-sends snd ws)]
          (when-not (or incoming sends)
            (Thread/sleep 100))))
      (finally
        (close snd)
        (close ws)
        (Thread/sleep 100)
        (info "closed worker sockets")))))

(defn start [ctx n]
  (reset! workers
          (into {} (map (fn [i] (swap! flags assoc i true) [i (future (worker ctx i))]) (range n)))))

(defn stop []
  (info "stopping worker")
  (doseq [[i w] @workers]
    (swap! flags assoc i false))
  (reset! flags {})
  (reset! workers {}))
