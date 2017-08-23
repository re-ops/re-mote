(ns user
  (:require
   [hawk.core :as hawk]
   [clojure.java.io :as io]
   [clojure.repl :refer :all]
   [re-mote.log :refer (refer-logging)]
   [clojure.tools.namespace.repl :refer (refresh refresh-all)]
   [re-mote.launch :as launch]
   [re-mote.repl :refer :all])
  (:import re_mote.repl.base.Hosts))

(refer-logging)

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

(declare go)

(defn refesh-on [ctx {:keys [kind]}]
  (when (= kind :create)
    (binding [*ns* (find-ns 'user)]
      (refresh)
      (stop)
      (go :watch false)))
  ctx)

(defn auto-reload []
  (println "watching src")
  (hawk/watch! [{:paths ["src"] :handler refesh-on}]))

(defn go
  "Initializes the current development system and starts it running."
  [& {:keys [watch]}]
  (when watch (auto-reload))
  (init)
  (start)
  (doseq [f (filter #(and (.isFile %) (.endsWith (.getName %) ".clj")) (file-seq (io/file "scripts")))]
    (load-file (.getPath f))))

(defn reset []
  (stop)
  (refresh :after 'user/go))

(defn clrs
  "clean repl"
  []
  (print (str (char 27) "[2J"))
  (print (str (char 27) "[;H")))
