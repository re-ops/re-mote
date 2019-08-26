(ns re-mote.zero.functions
  (:require
   [taoensso.timbre :refer  (refer-timbre)]
   [re-mote.zero.send :refer (send-)]
   [re-share.core :refer (gen-uuid)]
   [clojure.java.shell :refer [sh]]
   [re-cog.meta :refer (fn-meta)]
   [serializable.fn :as s]
   [me.raynes.fs :refer (list-dir tmpdir exists? file)]))

(refer-timbre)

; Misc
(def ^{:doc "A liveliness ping"} ping
  (s/fn [] :ok))

(defn refer-zero-fns []
  (require '[re-mote.zero.functions :as fns]))

(defn call
  "Launch a remote clojure serializable functions on zeromq hosts"
  [f args zhs]
  {:pre [(not (nil? zhs)) (or (= f ping) (-> f fn-meta :name))]}
  (let [uuid (gen-uuid)]
    (doseq [[hostname address] zhs]
      (send- address {:request :execute :uuid  uuid :fn f :args args :name (-> f fn-meta :name keyword)}))
    uuid))

(comment
  (processes-named "ssh"))
