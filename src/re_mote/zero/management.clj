(ns re-mote.zero.management
  "Managing client protocol"
  (:require
   [re-mote.zero.functions :as fns :refer (fn-meta)]
   [io.aviso.columns :refer  (format-columns write-rows)]
   [taoensso.timbre :refer  (refer-timbre)]
   [clojure.core.match :refer [match]]
   [puget.printer :as puget]
   [re-mote.zero.server :refer [send-]]))

(refer-timbre)

(def zmq-hosts (atom {}))

(def results (atom {}))

(defn fail [request e]
  {:response :fail :on request :cause e})

(defn register [{:keys [hostname uid] :as address}]
  (debug "register" hostname uid)
  (swap! zmq-hosts (fn [m] (assoc m hostname address))))

(defn unregister [{:keys [hostname uid] :as address}]
  (debug "unregister" hostname uid)
  (swap! zmq-hosts (fn [m] (dissoc m hostname))))

(defn ack [address request]
  (send- address {:response :ok :on (dissoc request :content)}))

(defn process
  "Process a message from a client"
  [{:keys [hostname uid] :as address} request]
  (try
    (debug "got" address request)
    (match [request]
      [{:request :register}] (ack address (register address))
      [{:request :unregister}] (ack address (unregister address))
      [{:reply :execute :result r :name name :uuid id}]
      (swap! results assoc-in [hostname (keyword name) id] r)
      :else (fail request "no handling clause found for request"))
    (catch Exception e
      (fail request e)
      (error e (.getMessage e)))))

(defn registered-hosts []
  (let [formatter (format-columns [:right 10] "  " [:right 20] "  " :none)]
    (write-rows *out* formatter [:hostname :uid :out] (vals @zmq-hosts))))

(defn result
  ([uuid k host]
   (get-in @results [host k uuid]))
  ([k host]
   (get-in @results [host k])))

(defn pretty-result
  "(pretty-result \"reops-0\" :plus-one)"
  [host k]
  (puget/cprint (result k host)))

(defn refer-zero-manage []
  (require '[re-mote.zero.management :as zerom :refer (registered-hosts pretty-result result)]))
