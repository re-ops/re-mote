(defproject re-mote "0.8.0"
  :description "A live remote operations environment"
  :url "https://github.com/re-ops/re-mote"
  :license  {:name "Apache License, Version 2.0" :url "http://www.apache.org/licenses/LICENSE-2.0.html"}
  :dependencies [
     [org.clojure/clojure "1.9.0"]
     [org.clojure/core.incubator "0.1.4"]
     [me.raynes/conch "0.8.0"]
     [org.clojure/core.async "0.3.443"]
     [com.rpl/specter "1.0.3"]
     [org.clojure/core.match "0.3.0-alpha4"]

     ; persistency
     [cc.qbits/spandex "0.5.5" :exclusions [org.clojure/clojure]]
     [org.apache.httpcomponents/httpclient "4.5.2"]

     ; pretty output
     [fipp "0.6.10"]
     [narkisr/clansi "1.2.0"]
     [mvxcvi/puget "1.0.1"]

     ; logging
     [com.taoensso/timbre "4.10.0"]
     [timbre-ns-pattern-level "0.1.2"]
     [com.fzakaria/slf4j-timbre "0.3.7"]
     [org.clojure/tools.trace "0.7.9"]

     ; repl
     [com.palletops/stevedore "0.8.0-beta.7"]
     [io.aviso/pretty "0.1.34"]
     [im.chit/lucid.package "1.3.13"]

     ; git repl
     [im.chit/lucid.git "1.3.13"]
     [org.eclipse.jgit "4.8.0.201706111038-r"]

     ; serialization
     [serializable-fn "1.1.4"]
     [org.clojure/data.codec "0.1.0"]
     [com.taoensso/nippy "2.13.0"]
     [cheshire "5.8.0"]

     ; remote execution
     [com.hierynomus/sshj "0.23.0" :exclusions [org.slf4j/slf4j-api]]
     [org.zeromq/jeromq "0.4.2"]

     ; model
     [clj-time/clj-time "0.13.0"]
     [im.chit/hara.data.map "2.5.10"]

     ; API
     [compojure "1.6.0"]
     [http-kit "2.2.0" :exclusions [ring/ring-jetty-adapter]]
     [ring "1.6.1"]
     [ring/ring-defaults "0.3.0"]
     [ring/ring-json  "0.4.0"]

     ; email
     [com.draines/postal "2.0.2"]

     ; CSS
     [org.webjars/bootstrap "3.3.5" :exclusions [org.slf4j/slf4j-api]]

     ; configuration
     [formation "0.2.0"]

     ; common utilities and shared functions
     [re-share "0.4.1"]
     [me.raynes/fs "1.4.6"]

     ; profiling
     [narkisr/clj-async-profiler "0.1.1"] ]

  :exclusions [org.clojure/clojure]

  :plugins  [[lein-jdk-tools "0.1.1"]
             [jonase/eastwood "0.2.4"]
             [lein-tag "0.1.0"]
             [lein-kibit "0.1.6"]
             [mvxcvi/whidbey "1.3.1"]
             [lein-ancient "0.6.7" :exclusions [org.clojure/clojure]]
             [lein-tar "2.0.0"]
             [lein-set-version "0.3.0"]
             [lein-cljfmt "0.5.6"]]


  :profiles {
    :dev {
       :source-paths  ["dev"]
     }

     :test {
        :jvm-opts ^:replace ["-Ddisable-conf=true"]
     }
   }

  :clean-targets [:target-path "out"]

  :repositories  {"bintray"  "http://dl.bintray.com/content/narkisr/narkisr-jars"}

  :repl-options {
    :init-ns user
    :prompt (fn [ns] (str "\u001B[35m[\u001B[34m" "re-mote" "\u001B[35m]\u001B[33mλ:\u001B[m " ))
    :welcome (println "Welcome to re-mote!" )
   }

  :aliases {
     "start-repl" ["do" "clean," "repl" ":headless"]
     "travis" [
      "with-profile" "test"  "do" "clean," "compile," "cljfmt" "check," "eastwood" "{:exclude-namespaces [re-mote.zero.worker re-mote.zero.server re-mote.zero.common re-mote.repl.spec]}"
     ]
     "start" ["do" "clean," "run"]
   }

  :target-path "target/"
  :signing {:gpg-key "narkisr@gmail.com"}
  :whidbey {
    :width 180
    :map-delimiter ""
    :extend-notation true
    :print-meta true
    :color-scheme {
      :delimiter [:blue]
       :tag [:bold :red]
    }
  }

)
