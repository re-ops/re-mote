(ns re-mote.repl.re-gent
  "Copy .curve server public key and run agent remotly"
  (:require
   [pallet.stevedore :refer (script chained-script)]
   [clojure.core.strint :refer (<<)]
   [re-mote.zero.server :refer (front-port)]
   [re-mote.repl.base :refer (run-hosts)])
  (:import [re_mote.repl.base Hosts]))

(defprotocol Regent
  (stop
    [this]
    [this m])
  (re-start
    [this]
    [this m home]))

(defn kill-script []
  (script
   (chain-or
    (pipe (pipe ("ps" "aux") ("awk" "'/re-gent/ {print $2}'")) ("xargs" "kill" "-9")) true)))

(defn start-script [port home]
  (let [bin (<< "~{home}/re-gent") cmd (<< "\"~{bin} ${IP} ~{port} &\"")]
    (script
     (set! IP @(pipe ("echo" "$SSH_CLIENT") ("awk" "'{print $1}'")))
     ("chmod" "+x"  ~bin)
     ("nohup" "sh" "-c" ~cmd "&>/dev/null")
     (if (not (= $? 0)) ("exit" 1)))))

(defn re-start-script [port home]
  (chained-script
   (kill-script)
   (start-script port home)))

(extend-type Hosts
  Regent
  (re-start [this]
    (launch this {}))
  (re-start [this _ home]
    [this (run-hosts this (re-start-script @front-port home))]))

(defn refer-regent []
  (require '[re-mote.repl.re-gent :as re-gent :refer (re-start stop)]))
