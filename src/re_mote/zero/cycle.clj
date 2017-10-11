(ns re-mote.zero.cycle
  (:require
   [taoensso.timbre :refer  (refer-timbre)]
   [re-share.zero.common :refer  (context close!)]
   [re-share.core :refer  (enable-waits stop-waits)]
   [re-mote.zero.management :as mgmt]
   [re-mote.zero.results :as res]
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
  (snd/start)
  (srv/start @ctx ".curve/server-private.key")
  (wrk/start @ctx 4)
  (enable-waits))

(defn stop []
  (stop-waits)
  (snd/stop)
  (when @ctx
    (wrk/stop)
    (srv/stop @ctx)
    (future
      (debug "terminating ctx")
      (let [c @ctx]
        (reset! ctx nil)
        (.term c)))
    (info "terminated ctx"))
  (res/clear-results)
  (mgmt/clear-registered))

(comment
  (start-zero-server))

