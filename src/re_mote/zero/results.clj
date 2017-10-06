(ns re-mote.zero.results
  "results collection and analyais"
  (:require
   [taoensso.timbre :refer  (refer-timbre)]
   [hara.data.map :refer (dissoc-in)]
   [puget.printer :as puget]))

(refer-timbre)

(def results (atom {}))

(defn add-result [hostname name id r t]
  (let [v {:r r :t t}]
    (if-let [a (get-in @results [(keyword name) id])]
      (swap! a assoc hostname v)
      (swap! results assoc-in [(keyword name) id] (atom {hostname v})))))

(defn- result [uuid k]
  (when-let [a (get-in @results [k uuid])] @a))

(defn clear-results
  [uuid k hs]
  (swap! results dissoc-in [k uuid]))

(defn get-results [{:keys [hosts]} k uuid]
  (let [ks (set (keys (result uuid k)))]
    (when (every? ks hosts)
      (debug "got all results for" k uuid)
      (result uuid k))))

(defn missing-results [{:keys [hosts]} k uuid]
  (filter (result uuid k) hosts))

(defn pretty-result
  "(pretty-result \"reops-0\" :plus-one)"
  [k uuid host]
  (puget/cprint
   (when-let [r (result k uuid)] (r host))))

(defn refer-zero-results []
  (require '[re-mote.zero.results :as zerors :refer (pretty-result clear-results add-result get-results missing-results)]))
