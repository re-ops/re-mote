(ns re-mote.repl.schedule
  "Schedule tasks"
  (:require
   [re-share.schedule :refer (status next-run local-str)]
   [clojure.set :refer (rename-keys)]
   [clansi.core :refer (style)]
   [io.aviso.columns :refer  (format-columns write-rows)]
   [io.aviso.ansi :refer :all]
   [clojure.core.strint :refer (<<)]
   [taoensso.timbre :refer (refer-timbre)])
  (:import [org.joda.time DateTimeConstants DateTimeZone DateTime]))

(refer-timbre)

(defn color-host [{:keys [host code] :as m}]
  (update m :host
          (fn [_] (if-not (zero? code) (style host :red) (style host :green)))))

(defn pretify [{:keys [result] :as m}]
  (select-keys (merge m result) [:host :code :out]))

(defn pretty [rs]
  (let [formatter (format-columns [:right 10] "  " [:right 4] "  " :none)]
    (write-rows formatter [:host :code :out]
                (map (comp color-host pretify)  rs))))

(defn last-run []
  (doseq [[k {:keys [result period time]}] @status]
    (when (and result (vector? result))
      (let [[_ {:keys [success failure]}] result]
        (println "\nResults of running" (name k) "on" (local-str time) ":")
        (pretty (map (fn [m] (rename-keys m {:error :out})) (flatten (vals failure))))
        (pretty success)))))

(defn all []
  (last-run)
  (println " ")
  (next-run))

