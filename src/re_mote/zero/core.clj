(ns re-mote.zero.core
  (:require
   [taoensso.timbre :refer  (refer-timbre)]
   [re-mote.zero.common :refer  (context)]
   [re-mote.zero.server :refer (setup-server kill-server! bind)]
   [re-mote.zero.worker :refer (setup-workers stop-workers!)]))

(refer-timbre)

(def ctx (atom nil))

(defn start-zero-server []
  (reset! ctx (context))
  (setup-server @ctx ".curve/server-private.key")
  (setup-workers @ctx 4)
  (future (bind)))

(defn stop-zero-server []
  (stop-workers!)
  (kill-server!)
  (when @ctx
    (.term @ctx))
  (reset! ctx nil))

(defn refer-zero []
  (require '[re-mote.zero.management :as zerom :refer (registered-hosts)]))

(comment
  (start-zero-server))

