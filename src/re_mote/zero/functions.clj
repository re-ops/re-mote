(ns re-mote.zero.functions
  (:require
   [taoensso.timbre :refer  (refer-timbre)]
   [cheshire.core :refer (parse-string)]
   [re-scan.core :refer [into-ports into-hosts nmap]]
   [re-mote.zero.send :refer (send-)]
   [re-mote.log :refer (gen-uuid)]
   [clojure.java.shell :refer [sh]]
   [serializable.fn :as s]
   [me.raynes.fs :refer (list-dir tmpdir exists? file)]))

(refer-timbre)

; Puppet/Facter
(def ^{:doc "Puppet facter facts"} facter
  (s/fn []
    (parse-string (:out (sh "facter" "--json")) true)))

; osquery
(def ^{:doc "Run an osquery query"} osquery
  (s/fn [query]
    (parse-string (:out (sh "/usr/bin/osqueryi" "--json" query)) true)))

; shell
(def ^{:doc "Excute a script using bash"} shell
  (s/fn [sum script]
    (let [f (file (tmpdir) sum)]
      (when-not (exists? f)
        (spit f script))
      (sh "bash" (.getPath f)))))

; Misc
(def ^{:doc "list dir"} listdir
  (s/fn [d]
    (map str (list-dir d))))

(def ^{:doc "adding one"} plus-one
  (s/fn [x] (inc x)))

(def ^{:doc "always fails"} fails
  (s/fn []
    (sh "fail")))

(def ^{:doc "A liveliness ping"} ping
  (s/fn [] :ok))

; Security
(def ^{:doc "Open ports nmap scan"} scan-ports
  (s/fn [path flags network]
    (apply merge (into-ports (nmap path flags network)))))

(def ^{:doc "Host addresses nmap scan"} scan-hosts
  (s/fn [path flags network]
    (into-hosts (nmap path flags network))))

(defn refer-zero-fns []
  (require '[re-mote.zero.functions :as fns :refer
             (pkg-update pkg-upgrade pkg-fix pkg-kill pkg-install fails
                         scan-hosts scan-ports shell plus-one operating-system hardware listdir call
                         all-processes processes-by named)]))

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
