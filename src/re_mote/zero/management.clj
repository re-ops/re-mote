(ns re-mote.zero.management
  "Managing client protocol"
  (:require
   [re-mote.zero.functions :as fns :refer (fn-meta)]
   [io.aviso.columns :refer  (format-columns write-rows)]
   [taoensso.timbre :refer  (refer-timbre)]
   [clojure.core.match :refer [match]]
   [re-mote.zero.server :refer [send-]]))

(refer-timbre)

(def hosts (atom {}))

(def results (atom {}))

(defn fail [request e]
  {:response :fail :on request :cause e})

(defn register [{:keys [hostname uid] :as address}]
  (debug "register" hostname uid)
  (swap! hosts (fn [m] (assoc m hostname address))))

(defn unregister [{:keys [hostname uid] :as address}]
  (debug "unregister" hostname uid)
  (swap! hosts (fn [m] (dissoc m hostname))))

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
      [{:reply :metrics :content m}] (swap! results assoc-in [hostname :metrics] m)
      [{:reply :execute :result r :name name}] (swap! results assoc-in [hostname name] r)
      :else (fail request "no handling clause found for request"))
    (catch Exception e
      (fail request e)
      (error e (.getMessage e)))))

(defn metrics []
  (doseq [[hostname address] @hosts]
    (send- address {:request :metrics})))

(defn registered-hosts []
  (let [formatter (format-columns [:right 10] "  " [:right 20] "  " :none)]
    (write-rows *out* formatter [:hostname :uid :out] (vals @hosts))))

(defn call
  "Launch a remote function on the cluster
    The function has to be created using s/fn"
  [f args]
  (doseq [[hostname address] @hosts]
    (send- address {:request :execute :fn f :args args :name (-> f fn-meta :name)})))

(comment
  (call fns/plus-one [1])
  (call fns/ls ["/"])
  (call fns/touch ["/tmp/bla"])
  (clojure.pprint/pprint @results)
  (clojure.pprint/pprint @hosts)
  (metrics)
  (reset! results {}))
