(ns re-mote.zero.functions
  (:require
   [cheshire.core :refer (parse-string)]
   [re-share.oshi :refer (read-metrics os)]
   [clojure.java.shell :refer [sh]]
   [serializable.fn :as s]
   [me.raynes.fs :refer (list-dir tmpdir exists? file)]))

(set! *warn-on-reflection* true)

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
(def ^{:doc "Getting all OS information using oshi"} oshi-os
  (s/fn []
    (get-in (read-metrics) [:operatingSystem])))

(def ^{:doc "Getting all Hardware information using oshi"} oshi-hardware
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
  (s/fn [x] (+ 1 x)))

(def ^{:doc "always fails"} fails
  (s/fn []
    (sh "fail")))

(defn refer-zero-fns []
  (require '[re-mote.zero.functions :as fns :refer
     (pkg-update pkg-upgrade pkg-fix pkg-kill pkg-install fails shell plus-one oshi-os oshi-hardware listdir)]))

(defn fn-meta [f]
  (meta
   (second
    (first
     (filter #(and (var? (second %)) (= f (var-get (second %)))) (ns-map 're-mote.zero.functions))))))

(comment)
