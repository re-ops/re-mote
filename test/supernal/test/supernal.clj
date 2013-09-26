(ns supernal.test.supernal
  (:use 
     midje.sweet
    [taoensso.timbre :only (warn debug)]
    [supernal.core :only (ns- execute env deref-all lifecycle apply-remote)]))

(fact "neted remotes application"
      (apply-remote '(let [1 2] (copy 1 2))) => '(let [1 2] ((copy 1 2) remote)))



(env 
  {:roles {
      :web #{{:host "192.168.2.26" :user "vagrant" :sudo true}}}})

(ns- hookable 
  (task runme (debug "here")))

(ns- error 
  (task zero-div :desc "meant to fail" (/ 1 0)))

(defn rollback-run [task] (debug "rolling back" task))

(ns- hookable-rollback
  (task run (rollback-run (args :failed-task))))

(lifecycle rollback {:doc "base deployment roleback"} {hookable-rollback/run #{}})

(lifecycle includes-error {:doc "Includes an error" :failure rollback} 
    {hookable/runme #{error/zero-div}})

(fact "failure hook"
  (execute includes-error {:app-name "foo" :src ""} :web) => (throws java.util.concurrent.ExecutionException) 
  (provided 
    (rollback-run #'supernal.user.error/zero-div)  => nil :times 1))

(fact "task description lookup"
   (:desc (meta supernal.user.error/zero-div)) => "meant to fail")
