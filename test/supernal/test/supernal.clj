(ns supernal.test.supernal
   (:use 
     midje.sweet
     supernal.baseline
     [supernal.core :only (apply-remote)]))

(fact "neted remotes application"
  (apply-remote '(let [1 2] (copy 1 2))) => '(let [1 2] ((copy 1 2) remote)))

(fact "task description lookup"
  (:desc (meta supernal.user.deploy/update-code)) => "updates deployed code")
