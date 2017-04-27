(ns user
  (:require
    [hawk.core :as hawk]
    [clojure.java.io :as io]
    [clojure.repl :refer :all]
    [clojure.tools.namespace.repl :refer (refresh refresh-all)]
    [re-mote.launch :as launch]
    [re-mote.repl :refer :all])
   (:import re_mote.repl.base.Hosts)  
  )

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

(defn refesh-on [ctx {:keys [kind]}] 
  (when (= kind :create) 
    (binding [*ns* (find-ns 'user)] (refresh))) ctx)

(defn auto-reload []
  (hawk/watch! [{:paths ["src"] :handler refesh-on }]))

(defn go
  "Initializes the current development system and starts it running."
  []
  (auto-reload)
  (init)
  (start))

(defn reset []
  (stop)
  (refresh :after 'user/go))


