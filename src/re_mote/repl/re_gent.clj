(ns re-mote.repl.re-gent
  "Copy .curve server public key and run agent remotly"
  (:require
   [pallet.stevedore :refer (script chained-script)]
   [clojure.core.strint :refer (<<)]
   [re-mote.zero.server :refer (front-port)]
   [re-mote.repl.base :refer (run-hosts)])
  (:import [re_mote.repl.base Hosts]))

(defprotocol Regent
  (kill-agent
    [this]
    [this m])
  (start-agent
    [this]
    [this m home]))

(defn kill-script []
  (script
   (pipe
    (pipe ("ps" "aux") ("awk" "'/re-gent -jar/  {print $2}'")) ("xargs" "kill" "-9"))))

(defn start-script [port home]
  (let [bin (<< "~{home}/re-gent") cmd (<< "\"~{bin} ${IP} ~{port} &\"")]
    (script
     (set! IP @(pipe ("echo" "$SSH_CLIENT") ("awk" "'{print $1}'")))
     ("chmod" "+x"  ~bin)
     ("nohup" "sh" "-c" ~cmd "&>/dev/null"))))

(extend-type Hosts
  Regent
  (kill-agent [this]
    (kill-agent this {}))
  (kill-agent [this _]
    [this (run-hosts this (kill-script))])
  (start-agent [this]
    (start-agent this {}))
  (start-agent [this _ home]
    [this (run-hosts this (start-script @front-port home))]))

(defn refer-regent []
  (require '[re-mote.repl.re-gent :as re-gent :refer (start-agent kill-agent)]))

