(defproject re-mote "0.10.8"
  :description "A live remote operations environment"
  :url "https://github.com/re-ops/re-mote"
  :license  {:name "Apache License, Version 2.0" :url "http://www.apache.org/licenses/LICENSE-2.0.html"}
  :dependencies [
     [org.clojure/clojure "1.10.0"]
     [org.clojure/core.incubator "0.1.4"]
     [me.raynes/conch "0.8.0"]
     [org.clojure/core.async "0.3.443"]
     [com.rpl/specter "1.1.2"]
     [org.clojure/core.match "0.3.0-alpha4"]

     ; persistency
     [org.apache.httpcomponents/httpclient "4.5.2"]

     ; pretty output
     [fipp "0.6.14"]
     [narkisr/clansi "1.2.0"]
     [mvxcvi/puget "1.1.0"]

     ; logging
     [com.taoensso/timbre "4.10.0"]
     [timbre-ns-pattern-level "0.1.2"]
     [com.fzakaria/slf4j-timbre "0.3.8"]
     [org.clojure/tools.trace "0.7.9"]

     ; clojure to bash
     [com.palletops/stevedore "0.8.0-beta.7"]

     ; pretty printing
     [io.aviso/pretty "0.1.37"]

     ; serialization
     [serializable-fn "1.1.4"]
     [org.clojure/data.codec "0.1.1"]
     [com.taoensso/nippy "2.14.0"]
     [cheshire "5.8.1"]

     ; remote execution
     [com.hierynomus/sshj "0.26.0" :exclusions [org.slf4j/slf4j-api]]
     [org.zeromq/jeromq "0.4.3"]

     ; model
     [clj-time/clj-time "0.15.1"]
     [im.chit/hara.data.map "2.5.10"]

     ; API
     [compojure "1.6.1"]
     [http-kit "2.3.0" :exclusions [ring/ring-jetty-adapter]]
     [ring "1.7.1"]
     [ring/ring-defaults "0.3.2"]
     [ring/ring-json  "0.4.0"]

     ; email
     [com.draines/postal "2.0.3"]

     ; configuration
     [formation "0.2.0"]

     ; common utilities and shared functions
     [re-share "0.10.0"]
     [re-scan "0.2.1"]

     ; persistency
     [rubber "0.3.4"]

     ; fs utilities
     [me.raynes/fs "1.4.6"]

     ; wiring
     [mount "0.1.15"]

     ; monitoring
     [riemann-clojure-client "0.5.0"]
    ]

  :exclusions [org.clojure/clojure]

  :plugins  [[jonase/eastwood "0.3.5"]
             [lein-tag "0.1.0"]
             [lein-kibit "0.1.6"]
             [lein-codox "0.10.3"]
             [lein-ancient "0.6.15" :exclusions [org.clojure/clojure]]
             [lein-set-version "0.3.0"]
             [lein-cljfmt "0.5.6"]]


  :profiles {
    :dev {
       :source-paths  ["dev"]
     }

     :test {
        :jvm-opts ^:replace ["-Ddisable-conf=true"]
     }
     :codox {
       :dependencies [[org.clojure/tools.reader "1.3.2"]
                      [codox-theme-rdash "0.1.2"]]
       :plugins [[lein-codox "0.10.3"]]
       :codox {:project {:name "re-mote"}
               :themes [:rdash]
               :source-paths ["src"]
               :source-uri "https://github.com/re-ops/re-mote/blob/master/{filepath}#L{line}"
       }
     }
   }

  :clean-targets [:target-path "out"]

  :repositories  {"bintray"  "https://dl.bintray.com/content/narkisr/narkisr-jars"}

  :repl-options {
    :init-ns user
    :prompt (fn [ns] (str "\u001B[35m[\u001B[34m" "re-mote" "\u001B[35m]\u001B[33mλ:\u001B[m " ))
    :welcome (println "Welcome to re-mote!" )
   }

  :aliases {
     "travis" [
      "with-profile" "test"  "do" "clean," "compile," "cljfmt" "check," "eastwood" "{:exclude-namespaces [re-mote.zero.worker re-mote.zero.server re-mote.zero.common re-mote.spec] :exclude-linters [:redefd-vars]}"
     ]
     "docs" [
         "with-profile" "codox" "do" "codox"
     ]
   }


  :codox {:metadata {:doc/format :markdown} :themes [:rdash]}

  :target-path "target/"

  :signing {:gpg-key "narkisr@gmail.com"}
)
