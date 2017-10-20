(ns re-mote.repl.spec
  "spec for results and pipelines outputs"
  (:require
   [clojure.spec.alpha :as s]))

(defn length [l]
  (fn [s] (>= (.length s) l)))

(s/def ::code integer?)

(s/def ::exit integer?)

(s/def ::out string?)

(s/def ::host string?)

(s/def ::uuid (s/and string? (length 32)))

(s/def ::time number?)

(s/def ::result
  (s/keys :opt-un [::out ::exit ::err]))

(s/def ::profile
  (s/keys
   :req-un [::time]))

(s/def ::success
  (s/coll-of
   (s/keys
    :opt-un [::stats ::uuid ::profile]
    :req-un [::code ::host ::result])))

(s/def ::error
  (s/keys :req-un [::out]))

(s/def ::fails
  (s/coll-of
   (s/keys
    :req-un [::code ::host ::uuid ::error])))

(s/def ::failure
  (s/map-of integer? ::fails))

(s/def ::hosts
  (s/coll-of string?))

(s/def ::operation-result
  (s/keys :req-un [::success ::failure ::hosts]))

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

(comment
  (def operation
    {:hosts ["foo" "bla"]
     :success [{:code 1 :result {:out "great!"} :host "foo" :uuid "6d7eaac200cb4786b05be428af18a06f"}]
     :failure {-1 [{:code -1 :host "bla" :error {:out "No route to host"} :uuid "f44f408b7fe24863b74b2a3d190f91f6"}]}})

  (s/explain ::operation-result example)
  (s/explain ::hosts-entity re-mote.repl/develop))
