(ns re-mote.spec
  "spec for results and pipelines outputs"
  (:require
   [clojure.core.strint :refer (<<)]
   [puget.printer :as puget]
   [taoensso.timbre :refer  (refer-timbre)]
   [expound.alpha :as expound]
   [clojure.spec.alpha :as s]))

(refer-timbre)

(defn length [l]
  (fn [s] (>= (.length s) l)))

(s/def ::code (s/and integer? #(and (>= % -1) (<= % 256))))

(s/def ::exit integer?)

(s/def ::out string?)

(s/def ::host string?)

(s/def ::uuid (s/and string? (length 32)))

(s/def ::time number?)

(s/def ::ssh-script
  (s/keys :req-un [::out ::code]))

(s/def ::single-fn
  (s/keys :req-un [::err ::out ::exit]))

(s/def ::end number?)

(s/def ::start number?)

(s/def ::type string?)

(s/def ::resource
  (s/merge (s/keys :req-un [::end ::start ::type ::uuid ::time]) ::single-fn))

(s/def ::resources
  (s/coll-of ::resource))

(s/def ::f keyword?)

(s/def ::plan
  (s/coll-of (s/keys :req-un [::resources])))

(s/def ::recipe
  (s/keys :req-un [::resources]))

; nmap

(defn port? [s]
  (let [p (Integer/parseInt s)]
    (and (>= p 1) (<= p 65536))))

(s/def :nmap/portid (s/and string? port?))

(s/def :nmap/state #{"open" "closed" "filtered" "unfiltered" "open/filtered" "closed/filtered"})

(s/def :nmap/single-port
  (s/keys :req-un [:nmap/portid :nmap/state]))

(s/def :nmap/ports
  (s/coll-of (s/keys :req-un [:nmap/portid :nmap/state])))

(s/def :nmap/scan (s/map-of keyword? :nmap/ports))

; cpu-vulns

(defn cpu-vuln? [s]
  (re-matches #"^(Mitigation:|Vulnerable|Not affected|KVM:).*\n$" s))

(s/def ::cpu-vulns (s/and string? cpu-vuln?))

(s/def :security/cpu-vulns
  (s/map-of keyword? ::cpu-vulns))

(s/def ::result
  (s/or
   :ssh ::ssh-script
   :zero ::single-fn
   :cog ::recipe
   :cog ::plan
   :security :security/cpu-vulns
   ; nmap transformations
   :nmap/raw :nmap/scan
   :nmap/split-hosts :nmap/ports
   :nmap/split-nested :nmap/single-port))

(s/def ::profile
  (s/keys
   :req-un [::time]))

(s/def ::success
  (s/coll-of
   (s/keys
    :opt-un [::uuid ::profile ::result]
    :req-un [::code ::host])))

(s/def ::error
  (s/keys :req-un [::out]))

(s/def ::fails
  (s/coll-of
   (s/keys
    :req-un [::code ::host ::uuid ::error])))

(s/def ::failure
  (s/map-of integer? ::fails))

(s/def ::hosts
  (s/nilable
   (s/coll-of string?)))

(s/def ::operation-result
  (s/keys
   :req-un [::success ::failure ::hosts]))

(s/def ::ssh-key string?)

(s/def ::user string?)

(s/def ::auth
  (s/keys
   :opt-un [::ssh-key]
   :req-un [::user]))

(s/def ::hosts-entity
  (s/keys :req-un [::auth ::hosts]))

(s/def ::pipeline
  (s/tuple ::hosts-entity ::operation-result))

(defn valid? [s v]
  (if-not (s/valid? s v)
    (let [e (expound/expound s v)]
      (error "spec failed:" e)
      (puget/cprint e)
      false)
    true))

(defn pipeline!
  "Checking the output of each function in a pipeline"
  [v]
  (when (nil? v)
    (throw (ex-info "Pipeline function returned a nil value!" {})))
  (if-not (s/valid? ::pipeline v)
    (let [exp (expound/expound ::pipeline v)]
      (throw (ex-info (<< "Pipline function output does not conform to the spec ~{exp}") {:explain exp :value v})))
    v))

(comment
  (require '[clojure.spec.gen.alpha :as gen])
  (gen/generate (s/gen ::pipeline)))
