(ns re-mote.zero.functions
  (:require
   [taoensso.timbre :refer  (refer-timbre)]
   [cheshire.core :refer (parse-string)]
   [re-share.oshi :refer (read-metrics os get-processes)]
   [re-scan.core :refer [open-ports nmap]]
   [re-mote.zero.send :refer (send-)]
   [re-mote.log :refer (gen-uuid)]
   [clojure.java.shell :refer [sh]]
   [serializable.fn :as s]
   [me.raynes.fs :refer (list-dir tmpdir exists? file)]))

(refer-timbre)

; Package manager
(def ^{:doc "update package manager"} pkg-update
  (s/fn []
    (case (os)
      :Ubuntu (sh "sudo" "apt" "update")
      :FreeBSD (sh "sudo" "pkg" "update")
      (throw (ex-info "not supported" {:os (os)})))))

(def ^{:doc "upgrade all packages"} pkg-upgrade
  (s/fn []
    (case (os)
      :Ubuntu (sh "sudo" "apt" "upgrade" "-y")
      :FreeBSD (sh "sudo" "pkg" "upgrade" "-y")
      (throw (ex-info "not supported" {:os (os)})))))

(def ^{:doc "install a package"} pkg-install
  (s/fn [pkg]
    (case (os)
      :Ubuntu (sh "sudo" "apt" "install" pkg "-y")
      :FreeBSD (sh "sudo" "pkg" "install" pkg "-y")
      (throw (ex-info "not supported" {:os (os)})))))

(def ^{:doc "Fix package provider"} pkg-fix
  (s/fn []
    (case (os)
      :Ubuntu (sh "sudo" "rm" "/var/lib/dpkg/lock" "/var/cache/apt/archives/lock")
      (throw (ex-info "not supported" {:os (os)})))))

(def ^{:doc "kill package provider"} pkg-kill
  (s/fn []
    (case (os)
      :Ubuntu (sh "sudo" "killall" "apt")
      (throw (ex-info "not supported" {:os (os)})))))

; OSHI
(def ^{:doc "Get all processes"} all-processes
  (s/fn []
    (get-processes)))

(def ^{:doc "Filter process by name"} named
  (fn [target]
    (s/fn [proc] (= (proc :name) target))))

(def ^{:doc "Get processes by fn"} processes-by
  (s/fn [f]
    (let [f' (eval f)]
      (filter f' (get-processes)))))

(def ^{:doc "Getting all OS information using oshi"} operating-system
  (s/fn []
    (get-in (read-metrics) [:operatingSystem])))

(def ^{:doc "Getting all Hardware information using oshi"} hardware
  (s/fn []
    (get-in (read-metrics) [:hardware])))

; Puppet/Facter
(def ^{:doc "Puppet facter facts"} facter
  (s/fn []
    (parse-string (:out (sh "facter" "--json")) true)))

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
(def ^{:doc "Nmap scan"} run-scan
  (s/fn [path flags network]
    (apply merge (open-ports (nmap path flags network)))))

(defn refer-zero-fns []
  (require '[re-mote.zero.functions :as fns :refer
             (pkg-update pkg-upgrade pkg-fix pkg-kill pkg-install fails
                         run-scan shell plus-one operating-system hardware listdir call
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
