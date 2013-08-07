(comment 
   Celestial, Copyright 2012 Ronen Narkis, narkisr.com
   Licensed under the Apache License,
   Version 2.0  (the "License") you may not use this file except in compliance with the License.
   You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.)

(ns supernal.launch
  (:use 
    [clansi.core :only (style)]
    [clojure.core.strint :only (<<)]) 
  (:refer-clojure :exclude  [list])
  (:require  [cliopatra.command :as command :refer  [defcommand]])
  (:gen-class true))

(defn list-tasks []
  (map (juxt identity (comp ns-publics symbol))
     (filter #(.startsWith % "supernal.user") (map #(-> % ns-name str) (all-ns)))))

(defn clear-prefix [ns-]
 (.replace ns- "supernal.user." ""))

(defn readable-form [[ns- ts]]
  (reduce (fn [r name*] (conj r (<< "~(clear-prefix ns-)/~{name*}" ))) #{} (keys ts)))

(defn task-exists? [full-name]
  (seq (filter #(% full-name) (map readable-form (into {} (list-tasks))))))

(defn get-cycles [] (deref (var-get (find-var 'supernal.core/cycles))))

(defn lifecycle-exists? [name*]
  ((into #{} (map #(-> % meta :name str ) (get-cycles))) name*))

(defmacro adhoc-eval [e]
   `(binding [*ns* (find-ns 'supernal.adhoc)] (eval ~e)))

(defn shout! [output]
   (println (style output :red))
   (System/exit 1))

(defn validated-args [args]
  (when-not (contains? args :app-name) (shout! "args must include :app-name (application name)"))
  (when-not (contains? args :src) (shout! "args must include :src (deployed content uri) )"))
  )

(defcommand run 
  " Run a single task or an entire lifecycle:

                  sup run {script} {task/lifecycle} -r {role} -a  \"{:src \"{uri}\", :app-name \"{name}\"}\"

                 * standalone tasks should be prefixed (deploy/start)."
  {:opts-spec [["-r" "--role" "Target Role" :required true]
               ["-a" "--args" "Task/Cycle arguments {:src \"uri\" :app-name \"name\"}" :default "{}"]]
   :bind-args-to [script name*]}
  (load-string (slurp script))
  (let [args* (read-string args)]
    (validated-args args*)
    (if (lifecycle-exists? name*)
     (adhoc-eval (clojure.core/list 'execute (symbol name*) args* (keyword role) :join true))
      (if (task-exists? name*) 
        (adhoc-eval (clojure.core/list 'execute-task (symbol name*) args* (keyword role) :join true)) 
        (shout! (<< "No matching lifecycle or task named ~{name*} found!"))))))

(defn print-tasks []
  (println (style "Tasks:" :blue))
  (doseq [[n ts] (list-tasks)]
    (println " " (style (<< "~(clear-prefix n):") :yellow))
    (doseq [[name* fn*] ts]
      (println "  " (style name* :green) (<< "- ~(:desc (meta (var-get fn*)))")))))

(defn print-cycles []
  (println (style "Lifecycles:" :blue))
  (doseq [c (get-cycles)]
    (let [{:keys [name]} (meta c) {:keys [doc]} (meta (var-get c))]
      (println " "  (style name :green) (<< "- ~{doc}")))))

(defcommand list
  "Lists available tasks and lifecycles:

                  sup list {script} 
  "
  {:opts-spec [] :bind-args-to [script]}
  (load-string (slurp script))
  (print-cycles) 
  (print-tasks))

(defcommand version 
  "List supernal version and info:

                  sup version 
  " 
  {:opts-spec [] :bind-args-to [script]}
  (println "Supernal 0.2.9"))

(defn -main [& args]
  (binding [*ns* (create-ns 'supernal.adhoc)] 
    (use '[clojure.core])
    (use '[supernal.core :only (ns- execute execute-task run copy env cycles)])
    (use '[supernal.baseline])
    (use '[taoensso.timbre :only (warn debug)]) 
    (command/dispatch 'supernal.launch args)))


(comment 
  (-main "run" "fixtures/supernal-demo.clj" "basic-deploy" "-r" "web") 
  )

