(ns re-mote.scripts.stats
  "Stats bash scripts"
  (:require
   [pallet.stevedore :refer (script do-script)]))

(defn net-script []
  (script
   (set! LC_ALL "en_AU.UTF-8")
   (set! IFC @(pipe ("ip" "route" "get" "1") ("awk" "'{print $5;exit}'")))
   (set! R @(("sar" "-n" "DEV" "1" "1")))
   (if (not (= $? 0)) ("exit" 1))
   (set! L @(pipe ((println (quoted "${R}"))) ("grep" (quoted "${IFC}"))))
   (pipe (pipe ((println (quoted "${L}"))) ("awk" "'NR==2 { print substr($0, index($0,$4)) }'")) ("tr" "-s" "' '"))))

(defn cpu-script []
  (script
   (set! LC_ALL "en_AU.UTF-8")
   (set! R @("LC_TIME=possix" "mpstat" "1" "1"))
   (if (not (= $? 0)) ("exit" 1))
   (pipe ((println (quoted "${R}"))) ("awk" "'NR==4 { print $3 \" \" $5 \" \" $12 }'"))))

(defn free-script []
  (script
   (set! R @("free" "-m"))
   (if (not (= $? 0)) ("exit" 1))
   (pipe ((println (quoted "${R}"))) ("awk" "'NR==2 { print $2 \" \" $3 \" \" $4 }'"))))

(defn load-script []
  (script
   (pipe ("uptime") ("awk" "-F" "'[, ]*'" "'NR==1 { print $(NF-2) \" \" $(NF-1) \" \" $(NF)}'"))))

(defn du-script []
  (script
   (set! R @("df" "-T" "-m"))
   (if (not (= $? 0)) ("exit" 1))
   (pipe (pipe ((println (quoted "${R}"))) ("awk" "'NR>1'")) ("tr" "-s" "' '"))))
