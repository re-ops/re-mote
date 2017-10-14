(ns re-mote.zero.events
  (:require
   [re-mote.zero.results :refer (missing-results)]
   [re-mote.zero.management :refer (all-hosts unregister)]
   [re-mote.zero.functions :refer (ping call)]
   [taoensso.timbre :refer  (refer-timbre)]))

(refer-timbre)

(defn handle [e-type event]
  (trace e-type (bean event))
  (when (= e-type :disconnected)
    (let [hosts (all-hosts) uuid (call ping [] hosts)]
      (Thread/sleep 1000)
      (doseq [absent (missing-results {:hosts (keys hosts)} uuid)]
        (debug absent "host is down!")
        (unregister (hosts absent))))))
