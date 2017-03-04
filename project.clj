(defproject supernal "0.6.1"
  :description "A remote multi server automation tool (like Capistrano/Fabric)"
  :url "https://github.com/celestial-ops/supernal"
  :license  {:name "Apache License, Version 2.0" :url "http://www.apache.org/licenses/LICENSE-2.0.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/core.incubator "0.1.4"]
                 [com.taoensso/timbre "4.1.4"]
                 [ch.qos.logback/logback-classic "1.2.1"]
                 [org.codehaus.groovy/groovy "2.4.9"]
                 [prismatic/plumbing "0.5.3"]
                 [com.hierynomus/sshj "0.20.0"]
                 [me.raynes/conch "0.8.0"]
                 [clj-aws-s3 "0.3.10"]
                 [org.clojure/core.async "0.3.441"]
                 [narkisr/cliopatra "1.1.0"]
                 [narkisr/clansi "1.2.0"]
                 ; tracking
                 [org.clojure/tools.trace "0.7.8"]
                 ; repl
                 [com.palletops/stevedore "0.8.0-beta.7"] 
                 [io.aviso/pretty "0.1.33"]
                 [progrock "0.1.1"]]

  :exclusions [org.clojure/clojure]

  :plugins  [[jonase/eastwood "0.0.2"] [lein-midje "3.1.3"] [lein-tag "0.1.0"]
             [lein-ancient "0.6.7" :exclusions [org.clojure/clojure]]
             [lein-tar "2.0.0"] [lein-set-version "0.3.0"] [lein-gorilla "0.4.0"]]

  :profiles {
    :dev {
       :dependencies [[midje "1.8.3"] [junit/junit "4.12"] ]
       :jvm-opts ~(vec (map (fn [[p v]] (str "-D" (name p) "=" v)) {:disable-conf "true"}))
       :resource-paths  ["pkg/etc/"]
       :source-paths  ["dev"]
       :set-version {
          :updates [{:path "src/supernal/launch.clj" :search-regex #"\"Supernal \d+\.\d+\.\d+\""}]
       }
     }
   }

  :repl-options {
    :init-ns user
  }

  :aliases {
     "autotest" ["midje" ":autotest" ":filter" "-integration"]
     "runtest" ["midje" ":filter" "-integration"]
     "supernal" ["run" "-m" "supernal.launch"]
   }

  :aot [supernal.launch]

  :target-path "target/"

  :signing {:gpg-key "narkisr@gmail.com"}

  :main supernal.launch
  )
