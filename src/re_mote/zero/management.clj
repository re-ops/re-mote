(ns re-mote.zero.management
  "Managing client protocol"
  (:require
   [table.core :refer (table)]
   [re-share.core :refer (error-m)]
   [re-mote.zero.results :refer (add-result)]
   [taoensso.timbre :refer  (refer-timbre)]
   [clojure.core.match :refer [match]])
  (:import
   [org.zeromq ZMQ ZMsg]))

(refer-timbre)

(def zmq-hosts (atom {}))

(defn all-hosts []
  @zmq-hosts)

(defn fail [request e]
  {:response :fail :on request :cause e})

(defn ack [address on]
  (info "acking" address)
  {:reply {:response :ok :on on}})

(defn register [{:keys [hostname uid] :as address}]
  (debug "register" hostname uid)
  (swap! zmq-hosts assoc hostname address)
  (ack address {:request :register}))

(defn unregister [{:keys [hostname uid]}]
  (debug "unregister" hostname uid)
  (swap! zmq-hosts dissoc hostname))

(defn process
  "Process a message from a client"
  [{:keys [hostname] :as address} request]
  (try
    (debug "got" address request)
    (match [request]
      [{:request "register"}] (register address)
      [{:request "unregister"}] (unregister address)
      [{:reply "execute" :result "failed" :name name :uuid id :error e}] (add-result hostname id e)
      [{:reply "execute" :result r :time t :name name :uuid id}] (add-result hostname id r t)
      :else (do
              (error "no handling clause found for request" request)
              (fail request "no handling clause found for request")))
    (catch Exception e
      (fail request e)
      (error-m e))))

(defn registered-hosts []
  (table (vals @zmq-hosts) :style :borderless))

(defn into-zmq-hosts
  "Get ZMQ addresses from Hosts"
  [{:keys [hosts]}]
  (select-keys @zmq-hosts hosts))

(defn clear-registered []
  (reset! zmq-hosts {}))

(defn refer-zero-manage []
  (require '[re-mote.zero.management :as zerom :refer (registered-hosts into-zmq-hosts clear-registered all-hosts)]))

