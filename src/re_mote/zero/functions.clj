(ns re-mote.zero.functions
  (:require
   [cheshire.core :refer (parse-string)]
   [re-share.oshi :refer (read-metrics os)]
   [clojure.java.shell :refer [sh]]
   [serializable.fn :as s]
   [me.raynes.fs :refer (list-dir temp-file delete)]))

(def ^{:doc "adding one"} plus-one
  (s/fn [x] (+ 1 x)))

(def ^{:doc "list dir"} ls
  (s/fn [d]
    (map str (list-dir d))))

(def ^{:doc "Excute a script using bash"} shell
  (s/fn [script]
    (let [f (temp-file "")]
      (try
        (spit f script)
        (sh "bash" (.getPath f))
        (finally
          (delete f))))))

(def ^{:doc "update package manager"} pkg-update
  (s/fn []
    (case (os)
      :Ubuntu (sh "sudo" "apt" "update")
      :FreeBSD (sh "sudo" "pkg" "update"))))

(def ^{:doc "upgrade all packages"} pkg-upgrade
  (s/fn []
    (case (os)
      :Ubuntu (sh "sudo" "apt" "upgrade" "-y")
      :FreeBSD (sh "sudo" "pkg" "upgrade" "-y"))))

(def ^{:doc "always fails"} fails
  (s/fn []
    (sh "fail")))

(def ^{:doc "Getting all OS information using oshi"} oshi-os
  (s/fn []
    (get-in (read-metrics) [:operatingSystem])))

(def ^{:doc "Getting all Hardware information using oshi"} oshi-hardware
  (s/fn []
    (get-in (read-metrics) [:hardware])))

(def ^{:doc "Puppet facter facts"} facter
  (s/fn []
    (parse-string (:out (sh "facter" "--json")) true)))

(defn refer-zero-fns []
  (require '[re-mote.zero.functions :as fns :refer (pkg-update pkg-upgrade fails shell plus-one oshi-os oshi-hardware)]))

(defn fn-meta [f]
  (meta
   (second
    (first
     (filter #(and (var? (second %)) (= f (var-get (second %)))) (ns-map 're-mote.zero.functions))))))

(comment
  (shell (re-mote.zero.stats/cpu-script))
  (clojure.pprint/pprint (:family (:operatingSystem (read-metrics)))))
