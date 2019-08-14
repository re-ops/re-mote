(ns re-mote.zero.worker
  (:require
   [re-mote.zero.send :refer (take-)]
   [taoensso.timbre :refer  (refer-timbre)]
   [clojure.data.codec.base64 :as b64]
   [cheshire.core :refer (parse-string)]
   [re-mote.zero.common :refer (send-)]
   [re-share.zero.common :refer (close)]
   [re-share.core :refer (error-m)]
   [taoensso.nippy :refer (thaw)]
   [re-mote.zero.management :refer (process)])
  (:import
   [com.google.json JsonSanitizer]
   [org.zeromq ZMQ ZMsg]))

(refer-timbre)

(def workers (atom {}))

(def flags (atom {}))

(defn worker-socket [ctx]
  (doto (.socket ctx ZMQ/DEALER)
    (.setLinger 0)
    (.connect "inproc://backend")))

(defn decode-message
  [bs]
  (parse-string (JsonSanitizer/sanitize (String. (b64/decode bs))) true))

(defn decode [msg]
  (-> msg (.pop) (.getData) decode-message))

(defn unpack [msg]
  (-> msg (.pop) (.getData) thaw))

(defn handle-incomming [socket]
  (try
    (when-let [msg (ZMsg/recvMsg socket ZMQ/DONTWAIT)]
      (let [address (unpack msg) content (decode msg)
            {:keys [hostname uid] :as m} address]
        (debug "got message from" hostname "uid" uid)
        (when-let [reply (:reply (process m content))]
          (send- socket address reply))
        true))
    (catch org.zeromq.ZMQException e
      (when (= (.getErrorCode e) 156384765)
        (throw e)))
    (catch Exception e
      (error-m e))))

(defn handle-sends [socket]
  (when-let [[address content] (take-)]
    (debug "sending" address content)
    (send- socket address content)
    true))

(defn worker [ctx i]
  (let [ws (worker-socket ctx)]
    (try
      (debug "worker running")
      (while (@flags i)
        (let [incoming (handle-incomming ws) sends (handle-sends ws)]
          (when-not (or incoming sends)
            (Thread/sleep 100))))
      (finally
        (close ws)
        (Thread/sleep 100)
        (debug "closed worker sockets")))))

(defn start [ctx n]
  (info "started workers")
  (reset! workers
          (into {} (map (fn [i] (swap! flags assoc i true) [i (future (worker ctx i))]) (range n)))))

(defn stop []
  (info "stopped workers")
  (doseq [[i w] @workers]
    (swap! flags assoc i false))
  (reset! flags {})
  (reset! workers {}))
