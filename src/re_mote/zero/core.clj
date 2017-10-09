(ns re-mote.zero.core
  (:require
   [taoensso.timbre :refer  (refer-timbre)]
   [re-share.zero.common :refer  (context close!)]
   [re-share.core :refer  (enable-waits stop-waits)]
   [re-mote.zero.management :refer  (clear-registered)]
   [re-mote.zero.server :as srv]
   [re-mote.zero.send :as snd]
   [re-mote.zero.worker :as wrk])
  (:import
   org.zeromq.ZMQ
   org.zeromq.ZMQ$Context))

(refer-timbre)

(def ctx (atom nil))

(defn start []
  (reset! ctx (context))
  (snd/start @ctx)
  (srv/start @ctx ".curve/server-private.key")
  (wrk/start @ctx 4)
  ;; (enable-waits)
)

(defn stop []
  ;; (stop-waits)
  (when @ctx
    (snd/stop @ctx)
    (wrk/stop)
    (srv/stop @ctx)
    (future
      (info "terminating ctx")
      (let [c @ctx]
        (reset! ctx nil)
        (.term c))
      (info "terminated ctx")))
  (clear-registered))

(defn refer-zero []
  (require '[re-mote.zero.management :as zerom :refer (registered-hosts)]))

(comment
  (start-zero-server))

