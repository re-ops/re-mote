(ns re-mote.zero.base
  "Base ns for zeromq pipeline support"
  (:require
    [re-mote.zero.management :refer (pretty-result results zmq-hosts)]
    [re-mote.zero.functions :as fns :refer (fn-meta)]
    [re-mote.zero.server :refer [send-]]
    [re-mote.log :refer (gen-uuid)]
    [re-share.core :refer (wait-for)]
    ))


(defn- into-zmq-hosts 
   "Get ZMQ addresses from Hosts" 
   [{:keys [hosts]}]
   (select-keys @zmq-hosts hosts))

(defn call
  "Launch a remote clojure function on hosts 
   The function has to support serialization"
  [f args hosts]
  (let [uuid (gen-uuid)]
    (doseq [[hostname address] (into-zmq-hosts hosts)]
      (send- address {:request :execute :uuid  uuid :fn f :args args :name (-> f fn-meta :name)}))
     uuid 
    ))

(defn collect 
   "Collect results from the zmq hosts blocking until all results are back" 
   [hosts uuid timeout]
   (wait-for timeout #() "Failed to collect all hosts") 
  )

(comment
  (pretty-result "enceladus" :processes)
  (call fns/processes [] re-mote.repl/sandbox)
  (call fns/plus-one [1] re-mote.repl/sandbox)
  (call fns/ls ["/"] re-mote.repl/sandbox)
  (call fns/touch ["/tmp/bla"])
  (call fns/apt-update [])
  (clojure.pprint/pprint @results)
  (clojure.pprint/pprint @hosts)
  (metrics)
  (reset! results {}))
