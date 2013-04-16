(ns supernal.integration.sshj
  "Basic sshj functionlity"
  (:use 
    midje.sweet
    [clojure.core.strint :only (<<)]
    [clojure.java.io :only (file)]
    [supernal.sshj :only (copy execute sh-)]))


(def remote {:host "192.168.1.26" :user "vagrant"})

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

