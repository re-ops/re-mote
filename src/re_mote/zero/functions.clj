(ns re-mote.zero.functions
  (:require
   [taoensso.timbre :refer  (refer-timbre)]
   [cheshire.core :refer (parse-string)]
   [re-mote.zero.send :refer (send-)]
   [re-mote.log :refer (gen-uuid)]
   [clojure.java.shell :refer [sh]]
   [serializable.fn :as s]
   [me.raynes.fs :refer (list-dir tmpdir exists? file)]))

(refer-timbre)

; osquery
(def ^{:doc "Run an osquery query"} osquery
  (s/fn [query]
    (parse-string (:out (sh "/usr/bin/osqueryi" "--json" query)) true)))

; Misc
(def ^{:doc "list dir"} listdir
  (s/fn [d]
    (map str (list-dir d))))

(def ^{:doc "always fails"} fails
  (s/fn []
    (sh "fail")))

(def ^{:doc "A liveliness ping"} ping
  (s/fn [] :ok))

(defn refer-zero-fns []
  (require '[re-mote.zero.functions :as fns :refer
             (shell listdir call named)]))

(defn fn-meta [f]
  (meta
   (second
    (first
     (filter #(and (var? (second %)) (= f (var-get (second %)))) (ns-map 're-mote.zero.functions))))))

(defn call
  "Launch a remote clojure serializable functions on zeromq hosts"
  [f args zhs]
  {:pre [(not (nil? zhs))]}
  (let [uuid (gen-uuid)]
    (doseq [[hostname address] zhs]
      (send- address {:request :execute :uuid  uuid :fn f :args args :name (-> f fn-meta :name)}))
    uuid))

(comment
  (processes-named "ssh"))
