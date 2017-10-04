(ns re-mote.zero.management
  "Managing client protocol"
  (:require
   [re-share.core :refer (error-m)]
   [re-mote.zero.functions :as fns :refer (fn-meta)]
   [io.aviso.columns :refer  (format-columns write-rows)]
   [taoensso.timbre :refer  (refer-timbre)]
   [clojure.core.match :refer [match]]
   [puget.printer :as puget]
   [re-mote.zero.frontend :refer [send-]]))

(refer-timbre)

(def zmq-hosts (atom {}))

(def results (atom {}))

(defn fail [request e]
  {:response :fail :on request :cause e})

(defn ack [address on]
  (info "acking" address)
  (send- address {:response :ok :on on}))

(defn register [{:keys [hostname uid] :as address}]
  (debug "register" hostname uid)
  (swap! zmq-hosts assoc hostname address)
  (ack address {:request :register}))

(defn unregister [{:keys [hostname uid] :as address}]
  (debug "unregister" hostname uid)
  (swap! zmq-hosts dissoc hostname)
  (ack address {:request :unregister}))

(defn reply [hostname name id r t]
  (swap! results assoc-in [hostname (keyword name) id] r))

(defn process
  "Process a message from a client"
  [{:keys [hostname uid] :as address} request]
  (try
    (trace "got" address request)
    (match [request]
      [{:request :register}] (register address)
      [{:request :unregister}] (unregister address)
      [{:reply :execute :result r :time t :name name :uuid id}] (reply hostname name id r t)
      :else (fail request "no handling clause found for request"))
    (catch Exception e
      (fail request e)
      (error-m e))))

(defn registered-hosts []
  (let [formatter (format-columns [:right 20] "  " [:right 10] "  " :none)]
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

(defn into-zmq-hosts
  "Get ZMQ addresses from Hosts"
  [{:keys [hosts]}]
  (select-keys @zmq-hosts hosts))

(defn clear-registered []
  (reset! zmq-hosts {}))

(defn refer-zero-manage []
  (require '[re-mote.zero.management :as zerom :refer (registered-hosts pretty-result result into-zmq-hosts clear-registered)]))
