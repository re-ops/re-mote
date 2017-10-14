(defproject re-mote "0.7.0"
  :description "A live remote operations environment"
  :url "https://github.com/re-ops/re-mote"
  :license  {:name "Apache License, Version 2.0" :url "http://www.apache.org/licenses/LICENSE-2.0.html"}
  :dependencies [
     [org.clojure/clojure "1.8.0"]
     [org.clojure/core.incubator "0.1.4"]
     [me.raynes/conch "0.8.0"]
     [org.clojure/core.async "0.3.443"]
     [com.rpl/specter "1.0.3"]

     ; in memory query
     [org.clojure/core.match "0.3.0-alpha4"]
     [datascript "0.16.2"]

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
     [com.hierynomus/sshj "0.21.1" :exclusions [org.slf4j/slf4j-api]]
     [org.zeromq/jeromq "0.4.2"]

     ; run at
     [jarohen/chime "0.2.1" :exclusions [org.clojure/core.async]]
     [clj-time/clj-time "0.13.0"]
     [im.chit/hara.data.map "2.5.10"]

     ;publishing
     [compojure "1.6.0"]
     [org.clojure/clojurescript "1.9.542"]
     [com.taoensso/sente "1.11.0"]
     [http-kit "2.2.0"]
     [ring "1.6.1"]
     [ring/ring-defaults "0.3.0"]
     [hiccup "1.0.5"]
     [com.draines/postal "2.0.2"]

     ; frontend
     [reagent "0.6.2"]
     [binaryage/devtools "0.9.4"]
     [metosin/vega-tools "0.2.0"]
     [ring-webjars "0.2.0"]
     [org.clojure/data.json "0.2.6"]

     ; CSS
     [org.webjars/bootstrap "3.3.5" :exclusions [org.slf4j/slf4j-api]]

     ; configuration
     [clojure-future-spec "1.9.0-alpha15"]
     [formation "0.2.0"]

     ; common utilities and shared functions
     [re-share "0.3.0"]
     [me.raynes/fs "1.4.6"]

    ]

  :exclusions [org.clojure/clojure]

  :plugins  [[jonase/eastwood "0.2.4"]
             [lein-tag "0.1.0"]
             [mvxcvi/whidbey "1.3.1"]
             [lein-ancient "0.6.7" :exclusions [org.clojure/clojure]]
             [lein-tar "2.0.0"]
             [lein-set-version "0.3.0"] [lein-gorilla "0.4.0"]
             [lein-figwheel "0.5.9"]
             [lein-cljfmt "0.5.6"]
             [lein-cljsbuild "1.1.4"]]

  :profiles {
    :dev {
       :source-paths  ["dev"]
     }
   }

  :clean-targets [:target-path "out"]

  :cljsbuild {
    :builds [
      {:id :cljs-client
       :source-paths ["src"]
       :compiler {
          :output-to "public/js/main.js" :optimizations :whitespace #_:advanced :pretty-print true
        }
       }]
  }


  :repositories  {"bintray"  "http://dl.bintray.com/content/narkisr/narkisr-jars"}

  :repl-options {
    :init-ns user
    :prompt (fn [ns] (str "\u001B[35m[\u001B[34m" "re-mote" "\u001B[35m]\u001B[33mλ:\u001B[m " ))
    :welcome (println "Welcome to re-mote!" )
   }

  :aliases {
     "start-repl" ["do" "clean," "cljsbuild" "once," "repl" ":headless"]
     "travis" [
        "do" "clean," "compile," "cljsbuild" "once," "cljfmt" "check," "eastwood" "{:exclude-namespaces [re-mote.zero.worker re-mote.zero.server re-mote.zero.common]}"
     ]
     "start" ["do" "clean," "cljsbuild" "once," "run"]
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
