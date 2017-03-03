(ns supernal.repl.stats
  (:require 
    [supernal.repl.base :refer (run-hosts)]
    [pallet.stevedore :refer (script)]
    )
  (:import [supernal.repl.base Hosts]))

(defprotocol Cpu
  (idle [this hosts])
  )

(extend-type Hosts
  Cpu
  (idle [this {:keys [hosts]}]
     (let [stat (script (pipe ("mpstat" "2" "1") ("awk" "'{ print $12 }'") ("tail" "-1")))]
       [this (run-hosts (:auth this) hosts stat)])))
