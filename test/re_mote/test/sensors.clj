(ns re-mote.test.sensors
  (:require
   [re-mote.zero.sensors :refer (assoc-stats)])
  (:use clojure.test))

(def intel-output
  "coretemp-isa-0000
Package id 0:
  temp1_input: 55.000
  temp1_max: 87.000
  temp1_crit: 105.000
  temp1_crit_alarm: 0.000
Core 0:
  temp2_input: 55.000
  temp2_max: 87.000
  temp2_crit: 105.000
  temp2_crit_alarm: 0.000
Core 1:
  temp3_input: 55.000
  temp3_max: 87.000
  temp3_crit: 105.000
  temp3_crit_alarm: 0.000
Core 2:
  temp4_input: 50.000
  temp4_max: 87.000
  temp4_crit: 105.000
  temp4_crit_alarm: 0.000
Core 3:
  temp5_input: 53.000
  temp5_max: 87.000
  temp5_crit: 105.000
  temp5_crit_alarm: 0.000")

(def amd-output
  "w83795adg-i2c-1-2f
3VSB:
  in13_input: 3.240
  in13_min: 2.964
  in13_max: 3.630
  in13_alarm: 0.000
fan1:
  fan1_input: 702.000
  fan1_min: 329.000
  fan1_alarm: 0.000
temp1:
  temp1_input: 38.750
  temp1_max: 109.000
  temp1_max_hyst: 109.000
  temp1_crit: 109.000
  temp1_crit_hyst: 109.000
  temp1_alarm: 0.000
  temp1_type: 3.000
temp2:
  temp2_input: 41.750
  temp2_max: 105.000
  temp2_max_hyst: 105.000
  temp2_crit: 105.000
  temp2_crit_hyst: 105.000
  temp2_alarm: 0.000
  temp2_type: 3.000
temp5:
  temp5_input: 18.250
  temp5_max: 39.000
  temp5_max_hyst: 39.000
  temp5_crit: 44.000
  temp5_crit_hyst: 44.000
  temp5_alarm: 0.000
  temp5_type: 4.000
intrusion0:
  intrusion0_alarm: 0.000

k10temp-pci-00c3
temp1:
  temp1_input: 43.000
  temp1_max: 70.000
  temp1_crit: 100.000
  temp1_crit_hyst: 95.000")

(def arm-output "39704")

(def expected-intel
  {:sensor {:coretemp-isa-0000 [{:device "Package id 0", :input 55.000M, :max 87.000M, :crit 105.000M, :alarm 0.000M}
                                {:device "Core 0", :input 55.000M, :max 87.000M, :crit 105.000M, :alarm 0.000M}
                                {:device "Core 1", :input 55.000M, :max 87.000M, :crit 105.000M, :alarm 0.000M}
                                {:device "Core 2", :input 50.000M, :max 87.000M, :crit 105.000M, :alarm 0.000M}
                                {:device "Core 3", :input 53.000M, :max 87.000M, :crit 105.000M, :alarm 0.000M}]}})

(def expected-amd
  {:sensor {:w83795adg-i2c-1-2f [{:device "3VSB", :input 3.240M, :min 2.964M, :max 3.630M, :alarm 0.000M}
                                 {:device "fan1", :input 702.000M, :min 329.000M, :alarm 0.000M}
                                 {:device "temp1", :input 38.750M, :max 109.000M, :hyst 109.000M, :crit 109.000M, :alarm 0.000M, :type 3.000M}
                                 {:device "temp2", :input 41.750M, :max 105.000M, :hyst 105.000M, :crit 105.000M, :alarm 0.000M, :type 3.000M}
                                 {:device "temp5", :input 18.250M, :max 39.000M, :hyst 44.000M, :crit 44.000M, :alarm 0.000M, :type 4.000M}
                                 {:device "intrusion0", :alarm 0.000M}]
            :k10temp-pci-00c3 [{:device "temp1", :input 43.000M, :max 70.000M, :crit 100.000M, :hyst 95.000M}]}})

(deftest sensors-parsing
  (is (= (:stats (assoc-stats {:host "foo" :result {:out intel-output}})) expected-intel))
  (is (= (:stats (assoc-stats {:host "foo" :result {:out amd-output}})) expected-amd)))
