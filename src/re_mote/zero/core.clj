(ns re-mote.zero.core
  (:require
   [taoensso.timbre :refer  (refer-timbre)]
   [re-mote.zero.common :refer  (context)]
   [re-mote.zero.management :refer  (clear-registered)]
   [re-mote.zero.server :refer (setup-server kill-server! bind-future)]
   [re-mote.zero.worker :refer (setup-workers stop-workers!)])
  (:import
   org.zeromq.ZMQ$Context))

(refer-timbre)

(def ctx (atom nil))

(defn start-zero-server []
  (reset! ctx (context))
  (setup-server @ctx ".curve/server-private.key")
  (setup-workers @ctx 4)
  (bind-future))

(defn stop-zero-server []
  (kill-server!)
  (stop-workers!)
  (info "terminating ctx")
  (when @ctx
    (.term ^ZMQ$Context @ctx)
    (reset! ctx nil))
  (clear-registered))

(defn refer-zero []
  (require '[re-mote.zero.management :as zerom :refer (registered-hosts)]))

(comment
  (start-zero-server))

