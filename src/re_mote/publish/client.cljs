(comment
   re-mote, Copyright 2017 Ronen Narkis, narkisr.com
   Licensed under the Apache License,
   Version 2.0  (the "License") you may not use this file except in compliance with the License.
   You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.)

(ns supernal.publish.client
  "client publish code" 
  (:require
   [clojure.string  :as str]
   [cljs.core.async :as async  :refer (<! >! put! chan)]
   [taoensso.encore :as encore :refer-macros (have have?)]
   [taoensso.timbre :as timbre :refer-macros (tracef debugf infof warnf errorf)]
   [taoensso.sente  :as sente  :refer (cb-success?)])
  (:require-macros
   [cljs.core.async.macros :as asyncm :refer (go go-loop)]))

;; (timbre/set-level! :trace) ; Uncomment for more logging

(def output-el (.getElementById js/document "output"))

(timbre/debug "ClojureScript appears to have loaded correctly.")

(let [{:keys [chsk ch-recv send-fn state]} 
      (sente/make-channel-socket-client!  "/chsk" {:type :ws :packer :edn})]
  (def chsk chsk)
  (def ch-chsk ch-recv) 
  (def chsk-send! send-fn) 
  (def chsk-state state))

(defmulti msg-handler :id)

(defn wrap-handler
  "Wraps `msg-handler` with logging, error catching, etc."
  [{:as ev-msg :keys [id ?data event]}]
  (timbre/debug event)
  (msg-handler ev-msg))

(defmethod msg-handler :default [{:as ev-msg :keys [event]}]
  (timbre/debug "Unhandled event: %s" event))

(defmethod msg-handler :chsk/state [{:as ev-msg :keys [?data]}]
  (let [[old-state-map new-state-map] (have vector? ?data)]
    (if (:first-open? new-state-map)
      (timbre/debug "Channel socket successfully established!: %s" new-state-map)
      (timbre/debug "Channel socket state change: %s"              new-state-map))))

(defmethod msg-handler :chsk/recv [{:as ev-msg :keys [?data]}]
  (supernal.publish.vega/update-graph (second ?data))
  (timbre/debug "Push event from server:" (second ?data)))

(defmethod msg-handler :chsk/handshake [{:as ev-msg :keys [?data]}]
  (let [[?uid ?csrf-token ?handshake-data] ?data]
    (timbre/debug "Handshake: %s" ?data)))

(defonce router_ (atom nil))

(defn  stop-router! [] (when-let [stop-f @router_] (stop-f)))

(defn start-router! []
  (stop-router!)
  (reset! router_
  (sente/start-client-chsk-router! ch-chsk wrap-handler)))

(defn start! [] (start-router!))

(defonce _start-once (start!))
