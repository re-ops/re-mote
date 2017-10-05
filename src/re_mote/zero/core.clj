(ns re-mote.zero.core
  (:require
   [taoensso.timbre :refer  (refer-timbre)]
   [re-share.zero.common :refer  (context)]
   [re-mote.zero.management :refer  (clear-registered)]
   [re-mote.zero.server :refer (setup-server kill-server! bind-future)]
   [re-mote.zero.frontend :refer (setup-front stop-front!)]
   [re-mote.zero.worker :refer (setup-workers stop-workers!)])
  (:import
   org.zeromq.ZMQ$Context))

(refer-timbre)

(def ctx (atom nil))

(defn start-zero-server []
  (reset! ctx (context))
  (let [private ".curve/server-private.key" frontend (setup-front @ctx private)]
    (setup-server @ctx private)
    (setup-workers @ctx 4)
    (bind-future frontend)))

(defn stop-zero-server []
  (stop-workers!)
  (stop-front!)
  (kill-server!)
  (Thread/sleep 1000)
  (info "terminating ctx")
  (when @ctx
    (.term ^ZMQ$Context @ctx)
    (reset! ctx nil))
  (clear-registered))

(defn refer-zero []
  (require '[re-mote.zero.management :as zerom :refer (registered-hosts)]))

(comment
  (start-zero-server))

