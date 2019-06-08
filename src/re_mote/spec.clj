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

(s/def ::shell-output
  (s/keys :req-un [::out]))

(s/def ::fn-output
  (s/or :maps (s/* map?) :map map?))

(s/def ::result
  (s/or :shell ::shell-output :fn ::fn-output))

(s/def ::profile
  (s/keys
   :req-un [::time]))

(s/def ::success
  (s/coll-of
   (s/keys
    :opt-un [::stats ::uuid ::profile ::result]
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
      (puget/cprint e))
    true))

(defn pipeline!
  "assert pipeline function output"
  [v]
  (if-not (s/valid? ::pipeline v)
    (let [exp (s/explain-data ::pipeline v)]
      (throw (ex-info (<< "function output is not valid ~{exp}") {:explain exp})))
    v))
