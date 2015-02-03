(defproject supernal "0.4.1"
  :description "A remote multi server automation tool (like Capistrano/Fabric)"
  :url "https://github.com/celestial-ops/supernal"
  :license  {:name "Apache License, Version 2.0" :url "http://www.apache.org/licenses/LICENSE-2.0.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.bouncycastle/bcprov-jdk16 "1.46"]
                 [org.clojure/core.incubator "0.1.2"]
                 [com.taoensso/timbre "1.5.2"]
                 [ch.qos.logback/logback-classic "1.0.9"]
                 [org.codehaus.groovy/groovy "2.1.2"]
                 [prismatic/plumbing "0.0.1"]
                 [net.schmizz/sshj "0.8.1"] 
                 [clj-aws-s3 "0.3.6"]
                 [me.raynes/conch "0.5.0"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]
                 [narkisr/cliopatra "1.1.0"]
                 [narkisr/clansi "1.2.0"] ]

  :exclusions [org.clojure/clojure]

  :plugins  [[jonase/eastwood "0.0.2"] [lein-midje "3.0.0"] [lein-tag "0.1.0"]
             [lein-tar "2.0.0"] [lein-set-version "0.3.0"] ]

  :profiles {:dev { 
              :dependencies [[midje "1.5.1"] [junit/junit "4.8.1"] ]
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
  
  :aliases {"autotest"
            ["midje" ":autotest" ":filter" "-integration"] 
            "runtest"
            ["midje" ":filter" "-integration"] 
            "supernal"
            ["run" "-m" "supernal.launch"] 
            }

  :aot [supernal.launch]

  :target-path "target/"

  :main supernal.launch
  )
