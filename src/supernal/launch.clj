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
  (:use [clojure.core.strint :only (<<)]) 
  (:refer-clojure :exclude  [list])
  (:require  [cliopatra.command :as command :refer  [defcommand]])
  (:gen-class true))

(defn list-tasks []
  (map (juxt identity (comp ns-publics symbol))
     (filter #(.startsWith % "supernal.user") (map #(-> % ns-name str) (all-ns)))))

(defn clear-prefix [ns-]
 (.replace ns- "supernal.user." ""))

(defn tasks-print []
   (doseq [[n ts] (list-tasks)]
     (println (clear-prefix n) ":") 
     (doseq [[name* fn*] ts]
       (println " " name* "-" (:desc (meta (var-get fn* ))))
       )))

(defn readable-form [[ns- ts]]
  (reduce (fn [r name*] (conj r (<< "~(clear-prefix ns-)/~{name*}" ))) #{} (keys ts)))

(defn task-exists? [full-name]
  (seq (filter #(% full-name) (map readable-form (into {} (list-tasks))))))

(defn lifecycle-exists? [name*]
  ((into #{} (map #(-> % meta :name str ) (deref (var-get (find-var 'supernal.core/cycles))))) name*))

(defmacro adhoc-eval [e]
   `(binding [*ns* (find-ns 'supernal.adhoc)] (eval ~e)))

(defcommand run 
  "Run a single task or an entire lifecycle"
  {:opts-spec [["-r" "--role" "Target Role" :required true]
               ["-a" "--args" "Task/Cycle arguments" :default "{}"]]
   :bind-args-to [script name*]}
  (load-string (slurp script))
  (let [args* (read-string args)]
    (when (lifecycle-exists? name*)
     (adhoc-eval (clojure.core/list 'execute (symbol name*) args* (keyword role) :join true))) 
    (when (task-exists? name*) 
      (adhoc-eval (clojure.core/list 'execute-task (symbol name*) args* (keyword role) :join true)))))


(defcommand list
  "List available tasks"
  {:opts-spec [] 
   :bind-args-to [script]}
  (load-string (slurp script))
  (tasks-print)
  )

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

