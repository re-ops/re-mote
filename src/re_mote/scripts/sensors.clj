(ns re-mote.scripts.sensors
  (:require
   [pallet.stevedore :refer (script do-script)]))

(defn vm-fail []
  (script
   (pipe ("cat" "/proc/cpuinfo") ("grep" "hypervisor"))
   (if (= "$?" 0)
     (do
       (println "'cannot measure temp in a VM'")
       ("exit" 1)))))

(defn fail! []
  (script
   (println "'no matching cpu type found'")
   ("exit" 1)))

(defn intel []
  (script
   (pipe ("cat" "/proc/cpuinfo") ("grep" "Intel"))
   (= "$?" 0)))

(defn arm []
  (script
   (pipe ("cat" "/proc/cpuinfo") ("grep" "ARM"))
   (= "$?" 0)))

(defn arm-cpu []
  (script
   (let t ("cat" "/sys/class/thermal/thermal_zone0/temp"))
   (/ t 1000)))

(defn sensors []
  (script
   (pipe ("sensors -A") ("awk" "'{$1=$1};1'"))))

(defn temp-script []
  (do-script
   (vm-fail)
   (if (intel)
     (sensors)
     (if (arm)
       (arm-cpu)
       (fail!)))))
