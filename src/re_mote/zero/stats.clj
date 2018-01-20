(ns re-mote.zero.stats
  "General machine stats"
  (:require
   [re-mote.scripts.stats :refer (net-script cpu-script free-script load-script du-script)]
   [clojure.core.strint :refer (<<)]
   [clojure.string :refer (split split-lines)]
   [clojure.tools.trace :as tr]
   [re-mote.zero.pipeline :refer (run-hosts)]
   [taoensso.timbre :refer (refer-timbre)]
   [com.rpl.specter :as s :refer (transform select MAP-VALS ALL ATOM keypath multi-path)]
   [clj-time.core :as t]
   [clj-time.coerce :refer (to-long)]
   [re-mote.zero.shell :refer (args)]
   [re-mote.zero.functions :refer (shell)]
   [re-share.schedule :refer (watch seconds)])
  (:import [re_mote.repl.base Hosts]))

(refer-timbre)

(defn space [line]
  (split line #"\s"))

(defn comma [line]
  (split line #","))

(defn zipped [parent k ks by {:keys [result] :as m}]
  (let [lines (split-lines (result :out))
        ms (mapv (fn [line] (zipmap ks (by line))) lines)]
    (assoc-in m [parent k]
              (if (> (count ms) 1) ms (first ms)))))

(defn zip
  "Collecting output into a hash, must be defined outside protocoal because of var args"
  [this {:keys [success failure] :as res} parent k & ks]
  (let [by (or (first (filter fn? ks)) space)
        success' (map (partial zipped parent k (filter keyword? ks) by) success)]
    [this (assoc (assoc res :success success') :failure failure)]))

(defprotocol Stats
  (du [this] [this m])
  (net [this] [this m])
  (cpu [this] [this m])
  (free [this] [this m])
  (load-avg [this] [this m])
  (collect [this m])
  (detect [this m f])
  (sliding [this m f k]))

(def readings (atom {}))

(defn safe-dec [v]
  (try
    (bigdec v)
    (catch Throwable e
      (error (<< "failed to convert {v} into big decimal") e))))

(def single-nav
  [:success ALL :stats MAP-VALS MAP-VALS])

(defn multi-nav [& ks]
  [:success ALL :stats MAP-VALS ALL (apply multi-path ks)])

(defn into-dec
  ([v]
   (into-dec single-nav v))
  ([nav [this readings]]
   [this (transform nav safe-dec readings)]))

(defn enrich [t [this readings]]
  [this (transform [:success ALL] (fn [m] (merge m {:timestamp (.getMillis (t/now)) :type t})) readings)])

(defn avg
  "Windowed average function"
  [ts]
  (let [sum (reduce (fn [a [t m]] (merge-with + a m)) {} ts)]
    {(-> ts first first) (transform [MAP-VALS] (fn [n] (with-precision 10 (/ n (count ts)))) sum)}))

(defn- window [f ts]
  (apply merge (map f (partition 3 1 ts))))

(defn reset
  "reset a key in readings"
  [k]
  (transform [ATOM MAP-VALS MAP-VALS] (fn [m] (dissoc m k)) readings))

(defn select-
  "select a single key from readings"
  [k]
  (select [ATOM MAP-VALS MAP-VALS (keypath k)] readings))

(defn last-n
  "keep last n items of a sorted map"
  [n m]
  (let [v (vec (into (sorted-map) m)) c (count v)]
    (if (< c n) m (into (sorted-map) (subvec v (- c n) c)))))

(def timeout [5 :second])

(defn disk-breach [limit]
  (fn [{:keys [host stats]}]
    [host (filter (fn [{:keys [perc]}] (> (safe-dec (subs perc 0 (- (.length perc) 1))) limit)) (stats :du))]))

(extend-type Hosts
  Stats
  (du
    ([this]
     (enrich "du"
             (into-dec (multi-nav :blocks :used :available)
                       (zip this
                            (run-hosts this shell (args du-script) timeout)
                            :stats :du :filesystem :type :blocks :used :available :perc :mount))))
    ([this _]
     (du this)))

  (net
    ([this _]
     (net this))
    ([this]
     (enrich "net"
             (into-dec
              (zip this (run-hosts this shell (args net-script) timeout)
                   :stats :net :rxpck/s :txpck/s :rxkB/s :txkB/s :rxcmp/s :txcmp/s :rxmcst/s :ifutil)))))
  (cpu
    ([this]
     (enrich "cpu" (into-dec (zip this (run-hosts this shell (args cpu-script) timeout) :stats :cpu :usr :sys :idle))))
    ([this _]
     (cpu this)))

  (free
    ([this]
     (enrich "free" (into-dec (zip this (run-hosts this shell (args free-script) timeout) :stats :free :total :used :free))))
    ([this _]
     (free this)))

  (load-avg
    ([this]
     (enrich "load" (into-dec (zip this (run-hosts this shell (args load-script) timeout) :stats :load :one :five :fifteen))))
    ([this _]
     (free this)))

  (detect [this {:keys [success] :as m} f]
    (let [breaches (filter (fn [[h bs]] (not (empty? bs))) (map f success)) detected (mapv first breaches)]
      [(Hosts. (:auth this) detected) (merge m {:hosts detected :breaches breaches})]))

  (collect [this {:keys [success] :as m}]
    (doseq [{:keys [host stats]} success]
      (doseq [[k v] stats]
        (swap! readings update-in [host k :timeseries]
               (fn [m] (if (nil? m) (sorted-map (t/now) v) (assoc m (t/now) v))))))
    [this m])

  (sliding [this {:keys [success] :as m} f fk]
    (doseq [{:keys [host stats]} success]
      (doseq [[k _] stats]
        (transform [ATOM (keypath host) k]
                   (fn [{:keys [timeseries] :as m}] (assoc m fk (window f timeseries))) readings)))
    [this m]))

(defn purge [n]
  (transform [ATOM MAP-VALS MAP-VALS MAP-VALS] (partial last-n n) readings))

(defn setup-stats
  "Setup stats collection"
  [s n]
  (watch :stats-purge (seconds s) (fn [] (purge n))))

(defn- host-values
  [k ks {:keys [host stats]}]
  (transform [ALL]
             (fn [[t s]] {:x (to-long t) :y (get-in s ks) :host host})
             (vec (get-in @readings [host (first (keys stats)) k]))))

(defn single-per-host
  "Collect a single nested reading for each host"
  [k ks success]
  (mapcat (partial host-values k ks) success))

(defn- avg-data-point [& ks]
  (let [[t _] (first ks) sums (apply (partial merge-with +) (map second ks))
        vs (transform [MAP-VALS] #(with-precision 10 (/ % (count ks))) sums)]
    (map (fn [[k v]] {:x (to-long t) :y v :c k}) vs)))

(defn avg-all
  "Average for all hosts"
  [k success]
  (let [r (first (keys (:stats (first success))))]
    (apply mapcat avg-data-point (select [ATOM MAP-VALS r k] readings))))

(defn refer-stats []
  (require '[re-mote.zero.stats :as stats :refer (load-avg net cpu free du detect disk-breach collect sliding setup-stats)]))

(comment
  (reset! readings {}))

