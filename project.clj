(defproject supernal "0.1.0"
  :description "A remote multi server automation tool (like Capistrano/Fabric)"
  :url "https://github.com/celestial-ops/supernal"
  :license  {:name "Apache License, Version 2.0" :url "http://www.apache.org/licenses/LICENSE-2.0.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/core.incubator "0.1.2"]
                 [com.taoensso/timbre "1.5.2"]
                 [org.bouncycastle/bcprov-jdk16 "1.46"]
                 [ch.qos.logback/logback-classic "1.0.9"]
                 [org.codehaus.groovy/groovy "2.1.2"]
                 [prismatic/plumbing "0.0.1"]
                 [pallet-thread "0.1.0"]
                 [net.schmizz/sshj "0.8.1"]]

  :exclusions [org.clojure/clojure]

  :plugins  [[jonase/eastwood "0.0.2"] [lein-pedantic "0.0.5"] [lein-midje "3.0.0"] [lein-bin "0.3.2"] ]


  :profiles {:dev 
             {:dependencies [[midje "1.5.1"] [junit/junit "4.8.1"] ]
              :jvm-opts ~(vec (map (fn [[p v]] (str "-D" (name p) "=" v)) {:disable-conf "true"}))}
             }


  :aliases {"autotest"
             ["midje" ":autotest" ":filter" "-integration"] 
             "runtest"
             ["midje" ":filter" "-integration"] 
             "supernal"
             ["run" "-m" "supernal.launch" "fixtures/supernal-demo.clj"] 
            }
 
  :bin {:name "supernal"}

  :aot [supernal.launch]

  :main supernal.launch
  )
