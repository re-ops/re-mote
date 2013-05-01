(env 
  {:roles {
      :web #{
             {:host "192.168.1.26" :user "vagrant" :sudo true}
             {:host "192.168.1.27" :user "vagrant" :sudo true}}}
   })

(ns- deploy 
  (task stop
    (debug "stopping service" remote)
    (run "hostname")))

;; (def artifact "git://github.com/narkisr/swag.git")

;; (execute basic-deploy {:app-name "foo" :src artifact} :web :join true)
