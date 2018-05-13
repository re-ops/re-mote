(ns re-mote.repl.output
  (:require
   [re-mote.log :refer (get-logs)]
   [clansi.core :refer (style)]
   [clojure.string :as s]
   [taoensso.timbre :refer (info)])
  (:import [re_mote.repl.base Hosts]))

(defprotocol Report
  (summary [this target])
  (log- [this m])
  (pretty
    [this title]
    [this m title]))

(defn summarize [s]
  (if s
    (let [l (.length s)]
      (if (< l 150)
        s
        (.substring s (- l 150) l)))
    ""))

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

  (pretty [this title]
    (pretty this {} title))

  (pretty [this {:keys [success failure] :as m} title]
    (println "")
    (println (style (str "Running " title " summary:") :blue) "\n")
    (when success
      (doseq [{:keys [host out]} success]
        (println " " (style "✔" :green) host (if out (summarize out) ""))))
    (when failure
      (doseq [[c rs] failure]
        (doseq [{:keys [host error]} (get-logs rs)]
          (let [{:keys [err out]} error s (if (empty? out) err out)]
            (println " " (style "x" :red) host "-" (str c) "," (summarize s))))))
    (println "")
    [this m]))

(defn refer-out []
  (require '[re-mote.repl.output :as out :refer (log- pretty)]))
