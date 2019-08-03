(ns re-mote.test.cog
  (:require
   [re-mote.repl.cog :refer (combine-results)])
  (:use clojure.test))

(def output-1 {:failure {-1 [{:code -1
                              :error {:out "host re-gent not connected"}
                              :host "a"
                              :uuid "4847d752d2b24cf28c57ffc06e569a7e"}]
                         -2 [{:code -2
                              :error {:out "No permissions"}
                              :host "b"
                              :uuid "4847d752d2b24cf2ffc06e569a7e5432"}]}
               :hosts ["a" "b" "c" "d"]
               :success [{:code 0
                          :host "c"
                          :profile {:time 0.091009881}
                          :result {:profile #{{:end 975525241689
                                               :result false
                                               :start 975525185967
                                               :time 5.5722E-5
                                               :type :directory
                                               :uuid "6b6cf853c6f24ad7b57122afe72fd3ed"}}}
                          :uuid "4847d752d2b24cf28c57ffc06e569a7e"}
                         {:code 0
                          :host "d"
                          :profile {:time 0.091009881}
                          :result {:profile #{{:end 975525241689
                                               :result false
                                               :start 975525185967
                                               :time 5.5722E-5
                                               :type :directory
                                               :uuid "6b6cf853c6f24ad7b57122afe72fd3ed"}}}
                          :uuid "4847d752d2b24cf28c57ffc06e569a7e"}]})

(def output-2 {:failure {-1 [{:code -1
                              :error {:out "host re-gent not connected"}
                              :host "c"
                              :uuid "5847d752d2b24cf28c57ffc06e569abc"}]}
               :hosts ["c" "d"]
               :success [{:code 0
                          :host "d"
                          :profile {:time 10}
                          :result {:profile #{{:end 975525241689
                                               :result {:out "download ok"}
                                               :start 975525185969
                                               :time 10
                                               :type :download
                                               :uuid "7b6cf853c6f24ad7b57122afe72fd3ed"}}}
                          :uuid "4847d752d2b24cf28c57ffc06e569a7e"}]})

(require '[com.rpl.specter :refer (ALL MAP-VALS transform select multi-path filterer)])

(let [f 're-cog.recipes.osquery/install]
  (transform [:success ALL :result] (fn [v] {f v}) output-1))

(let [f 're-cog.recipes.osquery/install]
  (into {} (transform [ALL] (fn [{:keys [result host]}] {host {f result}}) (:success output-1))))

{:code 0
 :host "d"
 :profile {:time 1}
 :result {:profile #{{:end 975525241689
                      :result false
                      :start 975525185967
                      :time 5.5722E-5
                      :type :directory
                      :uuid "6b6cf853c6f24ad7b57122afe72fd3ed"}}}
 :uuid "4847d752d2b24cf28c57ffc06e569a7e"}

{:code 0
 :host "d"
 :profile {:time 10}
 :result {:profile #{{:end 975525241689
                      :result {:out "download ok"}
                      :start 975525185969
                      :time 10
                      :type :download
                      :uuid "7b6cf853c6f24ad7b57122afe72fd3ed"}}}
 :uuid "4847d752d2b24cf28c57ffc06e569a7e"}

{:success
 [{:code 0
   :host "d"
   :profile {:time 11}
   :result {:re-cog.recipes.build/lein
            {:profile #{{:end 975525241689
                         :result false
                         :start 975525185967
                         :time 5.5722E-5
                         :type :directory
                         :uuid "6b6cf853c6f24ad7b57122afe72fd3ed"}}}
            :re-cog.recipes.osquery/install
            {:profile #{{:end 975525241689
                         :result false
                         :start 975525185969
                         :time 10
                         :type :download
                         :uuid "7b6cf853c6f24ad7b57122afe72fd3ed"}}}}}]}

(reduce (partial combine-results 're-cog.recipes.osquery/install) {:hosts ["a" "b" "c" "d"]} [output-1 output-2])

#_(deftest step-processing
    (is (= (merge-step {:succesful ["a" "b" "c" "d"]} output-1))))
