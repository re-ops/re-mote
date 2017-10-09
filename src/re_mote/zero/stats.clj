(ns re-mote.zero.stats
  "General stats"
  (:require
   [re-share.core :refer (md5)]
   [clojure.string :refer (split)]
   [clojure.tools.trace :as tr]
   [re-mote.zero.pipeline :refer (run-hosts)]
   [taoensso.timbre :refer (refer-timbre)]
   [com.rpl.specter :as s :refer (transform select MAP-VALS ALL ATOM keypath srange)]
   [clj-time.core :as t]
   [clj-time.coerce :refer (to-long)]
   [re-mote.zero.functions :refer (shell)]
   [re-mote.repl.schedule :refer (watch seconds)]
   [pallet.stevedore :refer (script do-script)])
  (:import [re_mote.repl.base Hosts]))

(refer-timbre)

(defn zipped [parent k ks {:keys [result] :as m}]
  (assoc-in m [parent k] (zipmap ks (split (get-in result [:r :out]) #"\s"))))

(defn zip
  "Collecting output into a hash, must be defined outside protocoal because of var args"
  [this {:keys [success failure] :as res} parent k & ks]
  (let [success' (map (partial zipped parent k ks) success)]
    [this (assoc (assoc res :success success') :failure failure)]))

(defprotocol Stats
  (net [this]
    [this m])
  (cpu [this]
    [this m])
  (free [this] [this m])
  (load-avg [this] [this m])
  (collect [this m])
  (sliding [this m f k]))

(defn bash!
  "check that we are running within bash!"
  []
  (script
   ("[" "!" "-n" "\"$BASH\"" "]" "&&" "echo" "Please set default user shell to bash" "&&" "exit" 1)))

(defn net-script []
  (script
   (set! LC_ALL "en_AU.UTF-8")
   (set! WHO @(pipe ("who" "am" "i") ("awk" "'{l = length($5) - 2; print substr($5, 2, l)}'")))
   (set! IFC @(pipe (pipe ((println (quoted "${WHO}"))) ("xargs" "ip" "route" "get")) ("awk" "'NR==1 {print $3}'")))
   (set! R @(("sar" "-n" "DEV" "1" "1")))
   (if (not (= $? 0)) ("exit" 1))
   (set! L @(pipe ((println (quoted "${R}"))) ("grep" (quoted "${IFC}"))))
   (pipe (pipe ((println (quoted "${L}"))) ("awk" "'NR==2 { print substr($0, index($0,$4)) }'")) ("tr" "-s" "' '"))))

(defn cpu-script []
  (script
   (set! LC_ALL "en_AU.UTF-8")
   (set! R @("LC_TIME=possix" "mpstat" "1" "1"))
   (if (not (= $? 0)) ("exit" 1))
   (pipe ((println (quoted "${R}"))) ("awk" "'NR==4 { print $3 \" \" $5 \" \" $12 }'"))))

(defn free-script []
  (script
   (set! R @("free" "-m"))
   (if (not (= $? 0)) ("exit" 1))
   (pipe ((println (quoted "${R}"))) ("awk" "'NR==2 { print $2 \" \" $3 \" \" $4 }'"))))

(defn load-script []
  (script
   (pipe ("uptime") ("awk" "-F" "'[, ]*'" "'NR==1 { print $(NF-2) \" \" $(NF-1) \" \" $(NF)}'"))))

(defn validate! [f]
  (do-script (bash!) (f)))

(def readings (atom {}))

(defn safe-dec [v]
  (try
    (bigdec v)
    (catch Throwable e
      (error v e))))

(defn into-dec [[this readings]]
  [this (transform [:success ALL :stats MAP-VALS MAP-VALS] safe-dec readings)])

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
  (let [v (into [] (into (sorted-map) m)) c (count v)]
    (if (< c n) m (into (sorted-map) (subvec v (- c n) c)))))

(def timeout [5 :second])

(defn args [bs]
  [(md5 (bs)) (validate! bs)])

(extend-type Hosts
  Stats
  (net
    ([this _]
     (net this))
    ([this]
     (into-dec
      (zip this (run-hosts this shell (args net-script) timeout)
           :stats :net :rxpck/s :txpck/s :rxkB/s :txkB/s :rxcmp/s :txcmp/s :rxmcst/s :ifutil))))

  (cpu
    ([this]
     (into-dec (zip this (run-hosts this shell (args cpu-script) timeout) :stats :cpu :usr :sys :idle)))
    ([this _]
     (cpu this)))

  (free
    ([this]
     (into-dec (zip this (run-hosts this shell (args free-script) timeout) :stats :free :total :used :free)))
    ([this _]
     (free this)))

  (load-avg
    ([this]
     (into-dec (zip this (run-hosts this shell (args load-script) timeout) :stats :load :one :five :fifteen)))
    ([this _]
     (free this)))

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
             (into [] (get-in @readings [host (first (keys stats)) k]))))

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
  (require '[re-mote.zero.stats :as stats :refer (load-avg net cpu free collect sliding avg setup-stats)]))

(comment
  (reset! readings {}))

