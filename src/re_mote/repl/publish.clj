(ns re-mote.repl.publish
  (:require
   ; publishing
   [postal.core :as p :refer (send-message)]
   [re-mote.publish.email :refer (template)]
   [re-share.config :as conf]
   [clojure.java.io :refer (file)]
   [clojure.core.strint :refer (<<)]
   [taoensso.timbre :refer (refer-timbre)]
   [re-mote.log :refer (gen-uuid get-logs)]
   [re-mote.repl.base :refer (refer-base)])
  (:import [re_mote.repl.base Hosts]))

(refer-timbre)

(defprotocol Publishing
  (email [this m e])
  (riemann [this m]))

(defn save-fails [{:keys [failure]}]
  (let [stdout (<< "/tmp/~(gen-uuid).out.txt") stderr (<< "/tmp/~(gen-uuid).err.txt")]
    (doseq [[c rs] failure]
      (doseq [{:keys [host error out]} (get-logs rs)]
        (do (spit stdout (str host ": " out "\n") :append true)
            (spit stderr (str host ": " error "\n") :append true))))
    [stdout stderr]))

(extend-type Hosts
  Publishing
  (email [this m e]
    (let [body {:type "text/html" :content (template m)}
          attachment (fn [f] {:type :attachment :content (file f)})
          files (map attachment (filter (fn [f] (.exists (file f))) (save-fails m)))
          m (merge e {:body (into [:alternative body] files)})]
      (send-message (conf/get! :re-mote :smtp) m)))
  (riemann [this {:keys [success failure]}]
    (println success)))

(defn refer-publish []
  (require '[re-mote.repl.publish :as pub :refer (email riemann)]))
