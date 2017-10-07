(ns re-mote.zero.results
  "results collection and analyais"
  (:require
   [taoensso.timbre :refer  (refer-timbre)]
   [hara.data.map :refer (dissoc-in)]
   [puget.printer :as puget]))

(refer-timbre)


(def buckets 32)

(def results 
  (into {} (map (fn [i] [i (atom {})]) (range buckets))))

(defn capacity []
  (map (fn [[k v]] (count @v)) results))

(defn bucket [uuid]
  (results (mod (BigInteger. uuid 16) buckets)))

(defn add-result [hostname name uuid r t]
  (let [v {:r r :t t} b (bucket uuid)]
    (if-let [a (get-in @b [(keyword name) uuid])]
      (swap! a assoc hostname v)
      (swap! b assoc-in [(keyword name) uuid] (atom {hostname v})))))

(defn result [uuid k]
  (if-let [a (get-in @(bucket uuid) [k uuid])] @a {}))

(defn clear-results
  [hs k uuid]
  (swap! (bucket uuid) dissoc-in [k uuid]))

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
   (let [r (result k uuid)] (r host))))

(defn refer-zero-results []
  (require '[re-mote.zero.results :as zerors :refer (pretty-result clear-results add-result get-results missing-results capacity)]))
