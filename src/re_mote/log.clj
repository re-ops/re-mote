(ns re-mote.log
  "log collection"
  (:require
   [timbre-ns-pattern-level :as level]
   [clojure.string :refer (join upper-case)]
   [taoensso.timbre.appenders.3rd-party.rolling :refer (rolling-appender)]
   [taoensso.timbre.appenders.core :refer (println-appender)]
   [clansi.core :refer (style)]
   [taoensso.timbre :refer (refer-timbre set-level! merge-config!)]
   [clojure.core.strint :refer (<<)]
   [chime :refer [chime-ch]]
   [clj-time.core :as t]
   [clj-time.coerce :refer [to-long]]
   [clojure.java.io :refer (reader)]
   [re-mote.repl.schedule :refer (watch seconds)]))

(refer-timbre)

(def logs (atom {}))

(def hosts (atom #{}))

(defn log-output
  "Output log stream"
  [out host]
  (doseq [line (line-seq (reader out))]
    (debug  (<< "[~{host}]:") line)))

(defn process-line
  "process a single log line"
  [host line]
  (when (or (@hosts host) (@hosts "*")) (info (<< "[~{host}]:") line)) line)

(defn collect-log
  "Collect log output into logs atom"
  [uuid]
  (fn [out host]
    (let [lines (doall (map (partial process-line host) (line-seq (reader out))))]
      (swap! logs (fn [m] (assoc m uuid  {:ts (t/now) :lines lines}))))))

(defn get-log
  "Getting log entry and clearing it"
  [uuid & clear]
  (when-let [{:keys [lines]} (get @logs uuid)]
    (when clear (swap! logs (fn [m] (dissoc m uuid))))
    lines))

(defn get-logs
  "Getting logs for all hosts"
  [hosts]
  (doall
   (map
    (fn [{:keys [uuid] :as m}]
      (if-not uuid m
              (dissoc (assoc m :out (join "\n" (get-log uuid))) :uuid))) hosts)))

(defn purge
  "Clearing dead non collected logs"
  []
  (let [minut-ago (to-long (t/minus (t/now) (t/minutes 1)))
        old (filter (fn [[uuid {:keys [ts]}]] (<= (to-long ts) minut-ago)) @logs)]
    (doseq [[uuid _] old]
      (trace "purged log" uuid)
      (swap! logs (fn [m] (dissoc m uuid))))
    :ok))

(defn run-purge [s]
  (watch :logs-purge (seconds s) (fn [] (trace "purging logs at" (t/now)) (purge))))

(defn gen-uuid []
  (.replace (str (java.util.UUID/randomUUID)) "-" ""))

(def level-color
  {:info :green :debug :blue :error :red :warn :yellow})

(defn output-fn
  "Timbre logger format function"
  ([data] (output-fn nil data))
  ([opts data] ; For partials
   (let [{:keys [level ?err #_vargs msg_ ?ns-str ?file hostname_ timestamp_ ?line]} data]
     (str (style (upper-case (name level)) (level-color level)) " " (force timestamp_) " [" (style ?file :bg-black) "@" ?line "] "  ": " (force msg_)))))

(defn- setup
  "See https://github.com/ptaoussanis/timbre"
  []
  ; disable-coloring
  (merge-config!
   {:output-fn (partial output-fn  {:stacktrace-fonts {}})})
  (merge-config!
   {:ns-blacklist ["net.schmizz.*"]})
  (merge-config! {:appenders {:println (merge {:ns-whitelist ["re-mote.output"]} (println-appender {:stream :auto}))
                              :rolling (rolling-appender {:path "re-mote.log" :pattern :weekly})}}))

(defn setup-logging
  "Sets up logging configuration:
    - stale logs removale interval
    - steam collect logs
    - log level
  "
  [& {:keys [interval level] :or {interval 10 level :info}}]
  (setup)
  (set-level! level)
  (run-purge interval))

(defn debug-on
  ([] (set-level! :debug))
  ([n]
   (merge-config! {:middleware [(level/middleware {n :debug})]})))

(defn debug-off []
  (set-level! :info))

(defn redirect-output [n]
  (merge-config! {
    :appenders {
      :println (merge {:ns-whitelist n} (println-appender {:stream :auto}))
   }}))

(defn log-hosts
  "Log a specific host by passing him as an argument
   Log all hosts by passing '*'
   Clearing all with an empty call"
  ([] (reset! hosts #{}))
  ([hs] (swap! hosts conj hs)))

(defn refer-logging []
  (require '[re-mote.log :as log :refer (debug-on debug-off log-hosts redirect-output)]))
