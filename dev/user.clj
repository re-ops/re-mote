(ns user
  (:require
   [lucid.git :refer [git]]
   [lucid.package :as lp]
   [clojure.java.io :as io]
   [clojure.repl :refer :all]
   [re-mote.log :refer (refer-logging)]
   [clojure.tools.namespace.repl :refer (refresh refresh-all)]
   [re-mote.launch :as launch]
   ; zero
   [re-mote.zero.management :refer (refer-zero-manage)]
   [re-mote.zero.results :refer (pretty-result)]
   [re-mote.zero.functions :refer (refer-zero-fns)]
   [re-mote.zero.facts :refer (refer-facts)]
   [re-mote.repl :refer :all])
  (:import
   java.io.File
   re_mote.repl.base.Hosts))

(refer-zero-manage)
(refer-facts)
(refer-logging)
(refer-zero-fns)

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
      (go)))
  ctx)

(defn go
  "Initializes the current development system and starts it running."
  []
  (init)
  (start)
  (doseq [f (filter (fn [^File v] (and (.isFile v) (.endsWith (.getName v) ".clj"))) (file-seq (io/file "scripts")))]
    (load-file (.getPath f))))

(defn reset []
  (stop)
  (refresh :after 'user/go))

(defn clrs
  "clean repl"
  []
  (print (str (char 27) "[2J"))
  (print (str (char 27) "[;H")))
