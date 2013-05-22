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

(ns supernal.core
  "A remote task execution framework (similar to fabric and capistrano) with follwing features:
  * Basic capistrano like functionality
  * A central server for running tasks
  * Clear seperation between tasks and execution/dependency order
  * Dynamic role and host lookup for tasks invocation targets
  * First class (java) package deployment lifecycle
  * can be used as a library and as a standalone tool  
  * Zeromq (jeromq) agent for improved perforemance over basic ssh
  "
  (:require 
       [clojure.walk :as walk]
       [pallet.thread.executor :as pallet]
       [supernal.sshj :as sshj]) 
  (:use 
    [taoensso.timbre :only (error)]
    [clojure.core.strint :only (<<)]
    [supernal.topsort :only (kahn-sort)] 
    [pallet.thread.executor :only (executor)]))


(defn gen-ns [ns*]
  (symbol (str "supernal.user." ns*)))

(defn apply-remote 
  "Applies call to partial copy and run functions under tasks,
  (copy foo bar) is transformed into ((copy from to) remote)"
  [body]
  (let [shim #{'run 'copy}]
    (walk/postwalk #(if (and (seq? %) (shim (first %))) (list % 'remote) %) body)))

(defn task 
  "Maps a task defition into a functions named name* under ns* namesapce" 
  [ns* name* body]
  (list 'intern (list symbol ns*) (list symbol name*) (concat '(fn [args remote]) (apply-remote body))))

(defmacro ns- 
  "Tasks ns macro, a group of tasks is associated with matching functions under the supernal.user ns"
  [ns* & tasks]
  `(do 
     (create-ns '~(gen-ns ns*))
     ~@(map 
         (fn [[_ name* & body]]
           (task (str (gen-ns ns*)) (str name*) body)) tasks))) 

(defn gen-lifecycle [ns*]
  (symbol (str ns* "-lifecycle")))

(defn resolve- [sym]
  (let [[pre k] (.split (str sym) "/")]
    (if-let [res (ns-resolve (symbol (str "supernal.user." pre)) (symbol k))]
      res
      (throw (Exception. (<< "No symbol ~{k} found in ns ~{pre}"))))))

(defmacro lifecycle 
  "Generates a topological sort from a lifecycle plan"
  [name* plan]
  `(def ~name*
     (with-meta
       (kahn-sort 
         (reduce (fn [r# [k# v#]] 
                   (assoc r# (resolve- k#) 
                          (into #{} (map #(resolve- %) v#)))) {} '~plan)) {:plan '~plan})))

(defn run-cycle [cycle* args remote]
  (try 
    (doseq [t cycle*]
     (t args remote))
    (catch Throwable e (error e))))

(defn run-id [args]
  (assoc args :run-id (java.util.UUID/randomUUID)))


(defmacro env-get 
  "Get instance list from env"
  [role opts-m]
  (if-let [env-m (opts-m :env)]
    `(get-in ~env-m [:roles ~role])
    `(get-in @~'env- [:roles ~role])))

(def pool
  (executor {:prefix "supernal" :thread-group-name "supernal" :pool-size 4 :daemon true}))

(defn bound-future [f]
 {:pre [(ifn? f)]}; saves lots of errors
  (pallet/execute pool f))

(defn wait-on [futures]
  "Waiting on a sequence of futures, limited by a constant pool of threads"
  (while (some identity (map (comp not future-done?) futures))
    (Thread/sleep 1000)
    )) 

(defmacro execute-template 
  "Executions template form"
  [role f opts] 
  (let [opts-m (apply hash-map opts) rsym (gensym)]
    (if (get opts-m :join true)
      `(wait-on 
         (map (fn [~rsym ] 
                (bound-future (fn [] ~(concat f (list rsym))))) (env-get ~role ~opts-m)))
      `(map (fn [~rsym ] 
              (bound-future (fn [] ~(concat f (list rsym))))) (env-get ~role ~opts-m))
      )))

(defmacro execute [name* args role & opts]
  "Executes a lifecycle defintion on a given role"
  `(execute-template ~role (run-cycle ~name* (run-id ~args)) ~opts))

(defmacro execute-task 
  "Executes a single task on a given role"
  [name* args role & opts]
  `(execute-template ~role ((resolve- '~name*) (run-id ~args)) ~opts))

(defmacro env 
  "A hash of running enviroment info and roles"
  [hash*]
  `(intern *ns* (symbol  "env-") (atom ~hash*)))

(defn role->hosts 
  "A hash based role to hosts resolver"
  [role] 
  )

(defn run
  "Running a given cmd on a remote system" 
  [cmd]
  (fn [remote] 
    (let [cmd* (if (-> remote :sudo) (str "sudo " cmd) cmd)]
      (sshj/execute cmd* remote))))

(defn copy
  "Copies src uri (either http/file/git) into a remote destination path" 
  [src dst]
  (partial sshj/copy src dst))

(defn has-keys? [m keys]
  (apply = (map count [keys (select-keys m keys)])))

(defn ssh-config 
   "Applies custom ssh configuration (key and user)" 
   [c] {:pre [(has-keys? c [:user :key])]}
  (reset! sshj/config c))
