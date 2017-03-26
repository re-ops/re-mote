(ns user
  (:use midje.repl)
  (:require
     [clojure.java.io :as io]
     [clojure.repl :refer :all]
     [clojure.tools.namespace.repl :refer (refresh refresh-all)]
     [re-mote.publish.server :as server]
     [re-mote.repl :as repl] 
     [re-mote.launch :as launch] 
     ))

(def system nil)

(defn init
  "Constructs the current development system."
  []
  (alter-var-root #'system (constantly (launch/setup))))

(defn start
  "Starts the current development system."
  []
  (alter-var-root #'system launch/start))

(defn stop
  "Shuts down and destroys the current development system."
  []
  (alter-var-root #'system launch/stop))

(defn go
  "Initializes the current development system and starts it running."
  []
  (init)
  (start))


(defn reset []
  (stop)
  (refresh :after 'user/go))

