(ns user
  (:require
   ; setup
   [re-share.zero.keys :as k]
   [re-share.config :as conf]
   [clojure.java.io :as io]
   [clojure.repl :refer :all]
   [re-mote.log :refer (refer-logging)]
   [re-share.log :refer (refer-share-logging)]
   [rubber.core :refer :all :exclude (call)]
   [clojure.tools.namespace.repl :refer (refresh refresh-all)]
   ; components
   [mount.core :as mount]
   [re-mote.zero.cycle :refer (zero)]
   [re-mote.persist.es :as es :refer (elastic)]
   [re-mote.publish.riemann :as r]
   [re-share.schedule :as sc]
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
(refer-share-logging)
(refer-zero-fns)

(defn start []
  (setup-logging)
  (k/create-server-keys ".curve")
  (conf/load (fn [_] {}))
  (mount/start #'elastic #'zero #'r/riemann)
  (es/initialize))

(defn stop []
  (sc/halt!)
  (mount/stop))

(declare go)

(defn refesh-on [ctx {:keys [kind]}]
  (when (= kind :create)
    (binding [*ns* (find-ns 'user)]
      (refresh)
      (stop)
      (go)))
  ctx)

(defn go
  []
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
