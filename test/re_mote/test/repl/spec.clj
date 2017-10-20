(ns re-mote.test.repl.spec
  (:require [re-mote.repl.spec :refer :all]))

(def operation
  {:hosts ["foo" "bla"]
   :success [{:code 1 :result {:out "great!"} :host "foo" :uuid "6d7eaac200cb4786b05be428af18a06f"}]
   :failure {-1 [{:code -1 :host "bla" :error {:out "No route to host"} :uuid "f44f408b7fe24863b74b2a3d190f91f6"}]}})

;; (s/explain ::operation-result operation)
;; (s/explain ::hosts-entity re-mote.repl/develop)
