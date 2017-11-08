(ns re-mote.repl.re-gent
  "Copy .curve server public key and run agent remotly"
  (:require
   [re-mote.zero.shell :refer (validate!)]
   [clojure.java.shell :refer (sh with-sh-dir)]
   [pallet.stevedore :refer (script chained-script)]
   [clojure.core.strint :refer (<<)]
   [re-mote.zero.server :refer (used-port)]
   [re-mote.ssh.pipeline :refer (run-hosts)])
  (:import [re_mote.repl.base Hosts]))

(defprotocol Regent
  (kill-agent
    [this]
    [this m])
  (start-agent
    [this home]
    [this m home]))

(defn kill-script []
  (let [flags "" position ""]
    (script
     (pipe ("cat" "/var/run/dmesg.boot") ("grep" "FreeBSD"))
     (if (= $? 0)
       (pipe
        (pipe
         (pipe ("ps" "-f") ("grep" "'[r]e-gent'")) ("awk" "'{print $1}'"))
        ("xargs" "kill" "-9")))
     (pipe ("cat" "/proc/version") ("grep" "Linux"))
     (if (= $? 0)
       (pipe
        (pipe
         (pipe ("ps" "ux") ("grep" "'[r]e-gent'")) ("awk" "'{print $2}'"))
        ("xargs" "kill" "-9"))))))

(defn start-script [port home level]
  (let [bin (<< "~{home}/re-gent") cmd (<< "\"~{bin} ${IP} ~{port} ~{level} &\"")]
    (script
     (set! IP @(pipe ("echo" "$SSH_CLIENT") ("awk" "'{print $1}'")))
     ("chmod" "+x"  ~bin)
     ("nohup" "sh" "-c" ~cmd "&>/dev/null"))))

(defn build []
  (with-sh-dir "../re-gent"
    (sh "bash" "bin/binary.sh")))

(extend-type Hosts
  Regent
  (kill-agent
    ([this _]
     [this (run-hosts this (kill-script))])
    ([this]
     (kill-agent this {})))

  (start-agent
    ([this _ home]
     [this (run-hosts this (start-script (used-port) home "debug"))])
    ([this home]
     (start-agent this nil home))))

(defn refer-regent []
  (require '[re-mote.repl.re-gent :as re-gent :refer (start-agent kill-agent build)]))

