(ns supernal.integration.tasks
  "Test basic dsl usage https://github.com/narkisr/puppet-supernal" 
  (:use 
    midje.sweet
    [supernal.baseline :only (base-deploy)]
    [taoensso.timbre :only (warn debug)]
    [supernal.core :only (ns- execute execute-task run copy env deref-all lifecycle)]))

(env 
  {:roles {
      :web #{{:host "192.168.2.26" :user "vagrant" :sudo true}}}
   })

(ns- deploy 
  (task stop
     (debug "here")
     (debug "stopping service" remote)
     (run "hostname")))

(ns- error
    (task zero-div
      (/ 1 0)))

(lifecycle includes-error {:doc "Includes an error"} {deploy/stop #{error/zero-div} })

(def artifact "git://github.com/narkisr/swag.git")

(fact "base deployment tasks no join" :integration :supernal
   (deref-all (execute base-deploy {:app-name "foo" :src artifact} :web :join false)) => nil)

(fact "single task" :integration :supernal
   (execute-task deploy/stop {:app-name "foo" :src artifact} :web) => nil)

(fact "env option" :integration :supernal
   (let [e {:roles {:web #{{:host "192.168.2.26" :user "vagrant" :sudo true}}} }]
     (execute-task deploy/stop {:app-name "foo" :src artifact} :web :env e) => nil))

(fact "joinless error" :integration :supernal
  (deref-all (execute-task error/zero-div {:app-name "foo" :src artifact} :web :join false)) =>
      (throws java.util.concurrent.ExecutionException))

(fact "task join with error" :integration :supernal
  (execute-task error/zero-div {:app-name "foo" :src artifact} :web) => (throws java.util.concurrent.ExecutionException))

(fact "lifecycle with join" :integration :supernal
   (execute includes-error {:app-name "foo" :src artifact} :web) => (throws java.util.concurrent.ExecutionException))
