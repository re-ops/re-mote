(ns re-mote.zero.results
  "results collection and analyais"
  (:require
   [taoensso.timbre :refer  (refer-timbre)]
   [hara.data.map :refer (dissoc-in)]
   [puget.printer :as puget]))

(refer-timbre)

(def buckets 32)

(def results
  (into {} (map (fn [i] [i (ref {})]) (range buckets))))

(defn capacity []
  (map (fn [[k v]] (count @v)) results))

(defn bucket [uuid]
  (results (mod (BigInteger. uuid 16) buckets)))

(defn add-result
  ([hostname uuid r]
   (let [v {:r r} b (bucket uuid)]
     (dosync
      (alter b assoc-in [uuid hostname] v))))
  ([hostname uuid r t]
   (let [v {:r r :t t} b (bucket uuid)]
     (dosync
      (alter b assoc-in [uuid hostname] v)))))

(defn result [uuid]
  (get @(bucket uuid) uuid {}))

(defn clear-results
  ([]
   (doseq [[k _] results]
     (dosync
      (ref-set (results k) {}))))
  ([uuid]
   (dosync
    (alter (bucket uuid) dissoc uuid))))

(defn get-results [hosts uuid]
  (let [ks (set (keys (result uuid)))]
    (when (every? ks hosts)
      (debug "got all results for" uuid)
      (result uuid))))

(defn missing-results [hosts uuid]
  (filter (comp not (result uuid)) hosts))

(defn pretty-result
  "(pretty-result \"reops-0\" :plus-one)"
  [uuid host]
  (puget/cprint
   (let [r (result uuid)] (r host))))

(defn refer-zero-results []
  (require '[re-mote.zero.results :as zerors :refer (pretty-result clear-results add-result get-results missing-results capacity)]))

(comment
  (println (capacity)))
