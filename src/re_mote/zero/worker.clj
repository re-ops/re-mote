(ns re-mote.zero.worker
  (:require
    [taoensso.timbre :refer  (refer-timbre)]
    [taoensso.nippy :as nippy :refer (freeze thaw)]
    [re-mote.zero.management :refer (process)])
  (:import
    [org.zeromq ZMQ ZMsg]))

(refer-timbre)

(defn worker-socket [ctx]
  (doto (.socket ctx ZMQ/DEALER)
    (.connect "inproc://backend")))

(def work (atom true))

(def workers (atom {}))

(defn handle-message [socket address content]
   (try
     (let [{:keys [hostname uid] :as m} (thaw address)]
       (debug "got message from" hostname "uid" uid)
       (process m (thaw content)))
     (catch Exception e
       (error e (.getMessage e)))))

(defn worker [ctx]
  (let [socket (worker-socket ctx)]
    (try
      (info "worker running")
      (while @work
         (let [msg (ZMsg/recvMsg socket) address (.pop msg) content (.pop msg)]
           (assert (not (nil? content)))
           (handle-message socket (.getData address) (.getData content))))
      (info "worker going down")
      (catch Exception e
        (error e (.getMessage e))))))

(defn setup-workers [ctx n]
  (reset! workers (into {} (map (fn [i] [i (future (worker ctx))]) (range n))))
  (reset! work true))

(defn stop-workers! []
  (reset! work false)
  (doseq [[i w] @workers]
    (future-cancel w))
  (reset! workers {}))
