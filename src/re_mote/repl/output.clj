(ns re-mote.repl.output
  (:require
   [re-mote.log :refer (get-logs)]
   [clansi.core :refer (style)]
   [clojure.string :as s]
   [taoensso.timbre :refer (refer-timbre)])
  (:import [re_mote.repl.base Hosts]))

(refer-timbre)

(defprotocol Report
  (summary [this target])
  (log- [this m])
  (pretty [this m]))

(defn summarize [s]
  (let [l (.length s)]
    (if (< l 50) s (.substring s (- l 50) l))))

(extend-type Hosts
  Report
  (log- [this {:keys [success failure] :as m}]
    (info "Successful:")
    (doseq [{:keys [host out] :as m} success]
      (doseq [line out] (info ">" host ":" line)))
    (info "Failures:")
    (doseq [[code hosts] failure]
      (info code " >")
      (doseq [{:keys [host]} hosts]
        (info  "  " host)))
    [this m])

  (pretty [this {:keys [success failure] :as m}]
    (println "")
    (println (style "Run summary:" :blue) "\n")
    (doseq [{:keys [host out]} success]
      (println " " (style "✔" :green) host (if out (summarize out) "")))
    (doseq [[c rs] failure]
      (doseq [{:keys [host error out]} (get-logs rs)]
        (println " " (style "x" :red) host "-" (if out (str c ",") "") (or error (summarize out)))))
    (println "")
    [this m]))

(defn refer-out []
  (require '[re-mote.repl.output :as out :refer (log- pretty)]))
