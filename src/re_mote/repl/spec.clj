(ns re-mote.repl.spec
  "spec for results and pipelines outputs"
  (:require
   [puget.printer :as puget]
   [taoensso.timbre :refer  (refer-timbre)]
   [clojure.spec.alpha :as s]))

(refer-timbre)

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
  (s/nilable
   (s/coll-of
    (s/keys
     :opt-un [::stats ::uuid ::profile ::result]
     :req-un [::code ::host]))))

(s/def ::error
  (s/keys :req-un [::out]))

(s/def ::fails
  (s/nilable
   (s/coll-of
    (s/keys
     :req-un [::code ::host ::uuid ::error]))))

(s/def ::failure
  (s/map-of integer? ::fails))

(s/def ::hosts
  (s/nilable
   (s/coll-of string?)))

(s/def ::operation-result
  (s/keys
   :req-un [::success ::failure ::hosts ::hosts]))

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
    (let [e (s/explain-data s v)]
      (info "spec failed:" e)
      (puget/cprint e))
    true))

(comment
  (def ok
    [{:auth {:ssh-key "foo" :user "bar"} :hosts ["one" "two"]}
     {:failure {-1
                [{:code -1 :error {:out "host re-gent not connected"} :host "one" :result {:r :failed} :uuid "87551290cd984ad9b4acfc9e24c4af80"}]}
      :hosts '("one" "two")
      :success '({:code 0
                  :host "two"
                  :result {:r {:err "" :exit 0 :out "2.01 1.51 96.48\n"} :t 1.014162418}
                  :stats {:cpu {:idle 96.48M :sys 1.51M :usr 2.01M}}
                  :uuid "87551290cd984ad9b4acfc9e24c4af80"})}])

  (s/valid? ::pipeline ok)
  (s/explain ::pipeline ok))
