(ns re-mote.repl.publish
  (:require
   [clojure.java.io :refer (file)]
   [clojure.core.strint :refer (<<)]
   [com.rpl.specter :as s :refer (transform select MAP-VALS ALL ATOM keypath srange)]
   [clojure.pprint :refer (pprint)]
   [taoensso.timbre :refer (refer-timbre)]
   [re-mote.publish.server :refer (broadcast!)]
   [re-mote.publish.email :refer (template)]
   [re-mote.zero.stats :refer (single-per-host avg-all readings)]
   [re-mote.log :refer (gen-uuid get-logs)]
   [postal.core :as p :refer (send-message)]
   [formation.core :as form]
   [re-mote.repl.base :refer (refer-base)])
  (:import [re_mote.repl.base Hosts]))

(refer-timbre)

(defprotocol Publishing
  (publish [this m e])
  (email [this m e]))

(def smtp
  (memoize (fn [] (:smtp (form/config "re-mote" (fn [_] nil))))))

(defn stock [n k & ks]
  {:graph {:gtype :vega/stock :gname n} :values-fn (partial single-per-host k ks)})

(defn lines [n]
  {:graph {:gtype :vega/lines :gname n} :values-fn identity})

(defn stack [n k]
  {:graph {:gtype :vega/stack :gname n} :values-fn (partial avg-all k)})

(defn save-fails [{:keys [failure]}]
  (let [stdout (<< "/tmp/~(gen-uuid).out.txt") stderr (<< "/tmp/~(gen-uuid).err.txt")]
    (doseq [[c rs] failure]
      (doseq [{:keys [host error out]} (get-logs rs)]
        (do (spit stdout (str host ": " out "\n") :append true)
            (spit stderr (str host ": " error "\n") :append true))))
    [stdout stderr]))

(extend-type Hosts
  Publishing
  (publish [this {:keys [success] :as m} {:keys [graph values-fn]}]
    (broadcast! [::vega {:values (sort-by :x (values-fn success)) :graph graph}])
    [this m])

  (email [this m e]
    (let [body {:type "text/html" :content (template m)}
          attachment (fn [f] {:type :attachment :content (file f)})
          files (map attachment (filter (fn [f] (.exists (file f))) (save-fails m)))]
      (send-message (smtp)
                    (merge e {:body (into [:alternative body] files)})))
    [this m]))

(defn refer-publish []
  (require '[re-mote.repl.publish :as pub :refer (publish email stock stack lines)]))

