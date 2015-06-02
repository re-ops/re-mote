(ns supernal.integration.sshj
  "Basic sshj functionlity"
  (:import clojure.lang.ExceptionInfo)
  (:require 
    [clojure.core.strint :refer (<<)]
    [clojure.java.io :refer (file)]
    [supernal.sshj :refer (copy execute sh-)])
  (:use 
    supernal.integration.common
    midje.sweet))


(def remote {:host (base-net ".26") :user "vagrant"})

(def git-uri "git://github.com/narkisr/cap-demo.git")

(def http-uri "http://dl.bintray.com/content/narkisr/boxes/redis-sandbox-0.3.4.tar.gz")

(def local-cap "/tmp/cap-demo")

(fact "git remote clone" :integration :sshj
      (copy git-uri "/tmp" remote) => nil
      (execute (<<  "rm -rf ~{local-cap}") remote))

(fact "remote http get" :integration :sshj
       (copy http-uri "/tmp" remote) => nil
       (execute "rm /tmp/redis-sandbox-0.3.4.tar.gz" remote))
 
(fact "local file copy to remote" :integration :sshj
      (copy "project.clj" "/tmp" remote) => nil 
      (execute "rm /tmp/project.clj" remote))

(fact "git local clone" :integration :sshj
      (copy "git://github.com/narkisr/cap-demo.git" "/tmp")
      (.exists (file "/tmp/cap-demo")) => truthy
      (sh- "rm" "-rf" "/tmp/cap-demo") )

(fact "local file copy to local" :integration :sshj
      (copy "project.clj" "/tmp") => nil
      (.exists (file "/tmp/project.clj")) => truthy
      (sh- "rm" "/tmp/project.clj"))

(defn with-m?  [m]
  (fn [actual]
    (= (.getMessage actual) m)))

(fact "shell timeout" :integration :shell
   (sh- "sleep" "10" {:timeout 1000}) => 
     (throws ExceptionInfo (with-m? "timed out while executing: sleep"))
   (sh- "sleep" "1") => nil
   (sh- "sleep" "-1") => (throws ExceptionInfo (with-m? "Failed to execute: sleep")))
