(ns re-mote.repl.stress
  (:require
   [clojure.core.strint :refer (<<)]
   [re-mote.repl.output :refer (refer-out)]
   [re-mote.repl.base :refer (refer-base)]))

(refer-base)
(refer-out)

(defn cpu-stress [hs c t]
  (run> (nohup hs (<< "/usr/bin/stress -c ~{c} -t ~{t}")) | (pretty "cpu stress started")))

(defn refer-stress []
  (require '[re-mote.repl.stress :as stress :refer (cpu-stress)]))
