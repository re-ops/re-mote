(ns re-mote.repl.schedule
  "Schedule tasks"
  (:require
   [clojure.set :refer (rename-keys)]
   [clj-time.format :as f]
   [clansi.core :refer (style)]
   [clojure.pprint :refer [pprint print-table]]
   [io.aviso.columns :refer  (format-columns write-rows)]
   [io.aviso.ansi :refer :all]
   [clj-time.periodic :refer  [periodic-seq]]
   [clojure.core.strint :refer (<<)]
   [taoensso.timbre :refer (refer-timbre)]
   [chime :refer [chime-at]]
   [clj-time.coerce :as c]
   [clj-time.core :as t]
   [clj-time.local :refer [local-now to-local-date-time]]
   [clojure.core.async :as a :refer [<! go-loop close!]])
  (:import [org.joda.time DateTimeConstants DateTimeZone DateTime]))

(refer-timbre)

(def chs (atom {}))
(def status (atom {}))

(defn in [s]
  [(-> s t/seconds t/from-now)])

(defn seconds
  ([n f] (periodic-seq (t/plus (local-now) (t/seconds f)) (-> n t/seconds)))
  ([n] (periodic-seq (local-now) (-> n t/seconds))))

(defn every-day [hour]
  (let [^DateTime now (local-now) dates (periodic-seq (.. now (withTime hour 0 0 0)) (t/days 1))]
    (if (> (c/to-long (first dates)) (c/to-long now)) dates (rest dates))))

(defn on-weekdays [hour]
  (->> (every-day hour)
       (remove (comp #{DateTimeConstants/SATURDAY DateTimeConstants/SUNDAY} #(.getDayOfWeek ^DateTime %)))))

(defn at-day [day hour]
  (->> (every-day hour) (filter (comp #{day} #(.getDayOfWeek ^DateTime %)))))

(defn watch
  "run f using provided period"
  [k period f & args]
  (swap! status assoc k {:period period})
  (swap! chs assoc k
         (chime-at period
                   (fn [t]
                     (trace "chime" t)
                     (let [result (apply f args)]
                       (swap! status update k
                              (fn [{:keys [period] :as m}] (merge m {:result result :time (local-now) :period (rest period)})))))
                   {:on-finished (fn [] (debug "job done" k))})))

(defn halt!
  ([]
   (doseq [[k f] @chs] (halt! k)))
  ([k]
   (debug "closing channel")
   ((@chs k))
   (debug "clearing chs and status atoms")
   (swap! chs (fn [curr] (dissoc curr k)))
   (swap! status (fn [curr] (dissoc curr k)))))

(defn local-str [t]
  (f/unparse (f/formatter-local "dd/MM/YY HH:mm:ss") t))

(defn color-host [{:keys [host code] :as m}]
  (update m :host (fn [_] (if-not (= code 0) (style host :red) (style host :green)))))

(defn pretty [rs]
  (let [formatter (format-columns [:right 10] "  " [:right 4] "  " :none)]
    (write-rows *out* formatter [:host :code :out]
      (map (fn [{:keys [result] :as m}] (color-host (merge (select-keys (merge m result) [:host :code :out])))) rs))))

(defn last-run []
  (doseq [[k {:keys [result period time]}] @status]
    (when (and result (vector? result))
      (let [[_ {:keys [success failure]}] result]
        (println "\nResults of running" (name k) "on" (local-str time) ":")
        (pretty (map (fn [m] (rename-keys m {:error :out})) (flatten (vals failure))))
        (pretty success)))))

(defn next-run []
  (doseq [[k {:keys [result period]}] (sort-by (fn [[k m]] (first (m :period))) @status)]
    (let [date (local-str (first period))]
      (println (style date :blue) (<< " ~(name k)")))))

(defn all []
  (last-run)
  (println " ")
  (next-run))

