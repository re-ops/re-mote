(env 
  {:roles {
      :web #{
             {:host "192.168.2.26" :user "vagrant" :sudo true}
             {:host "192.168.2.27" :user "vagrant" :sudo true}}}
   })

(ns- deploy 
  (task stop :desc "Stoping server foo"
    (debug "stopping service" remote)
    (run "hostname")))

(ns- play
  (task stop :desc "Stoping server foo"
    (debug "stopping service" remote)
    (run "hostname"))
 )

(lifecycle ex {:doc "just an example"} 
  {play/stop #{}})
