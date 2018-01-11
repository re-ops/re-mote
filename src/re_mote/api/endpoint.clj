(ns re-mote.api.endpoint
  "Functions as an endpoint"
  (:require
    [re-mote.repl :refer :all]))

(defn bsd-cpu []
  (cpu-persist bsd)
  )
