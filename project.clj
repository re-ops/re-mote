(defproject re-mote "0.1.0"
  :description "A live remote operations environment"
  :url "https://github.com/re-ops/re-mote"
  :license  {:name "Apache License, Version 2.0" :url "http://www.apache.org/licenses/LICENSE-2.0.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/core.incubator "0.1.4"]
                 [me.raynes/conch "0.8.0"]
                 [org.clojure/core.async "0.3.442"]
                 [narkisr/cliopatra "1.1.0"]
                 [narkisr/clansi "1.2.0"]
                 [com.rpl/specter "1.0.0"]

                 ; logging
                 [ch.qos.logback/logback-classic "1.2.1"]
                 [org.codehaus.groovy/groovy "2.4.9"]
                 [com.taoensso/timbre "4.10.0"]
                 [org.clojure/tools.trace "0.7.9"]

                 ; repl
                 [com.palletops/stevedore "0.8.0-beta.7"]
                 [io.aviso/pretty "0.1.33"]
                 [progrock "0.1.2"]

                 ; ssh
                 [com.hierynomus/sshj "0.20.0"]

                 ; run at
                 [jarohen/chime "0.2.0" :exclusions [org.clojure/core.async]]
                 [clj-time/clj-time "0.13.0"]

                 ; web publish
                 [compojure "1.5.2"]
                 [org.clojure/clojurescript "1.9.495"]
                 [com.taoensso/sente "1.11.0"]
                 [http-kit "2.2.0"]
                 [ring "1.5.1"]
                 [ring/ring-defaults "0.2.3"]
                 ;; [com.taoensso/encore "2.90.1"]
                 [hiccup "1.0.5"]

                 ; frontend
                 [reagent "0.6.1"]
                 [binaryage/devtools "0.9.2"]
                 [metosin/vega-tools "0.2.0"]
                 [ring-webjars "0.1.1"]
                 [org.clojure/data.json "0.2.6"]

                 ;; CSS
                 [org.webjars/bootstrap "3.3.5"]
                 ]

  :exclusions [org.clojure/clojure]

  :plugins  [[jonase/eastwood "0.0.2"] [lein-midje "3.1.3"] [lein-tag "0.1.0"]
             [lein-ancient "0.6.7" :exclusions [org.clojure/clojure]]
             [lein-tar "2.0.0"] [lein-set-version "0.3.0"] [lein-gorilla "0.4.0"]
             [lein-figwheel "0.5.9"] [lein-cljsbuild "1.1.4"]]

  :profiles {
    :dev {
       :dependencies [[midje "1.8.3"] [junit/junit "4.12"] ]
       :jvm-opts ~(vec (map (fn [[p v]] (str "-D" (name p) "=" v)) {:disable-conf "true"}))
       :resource-paths  ["pkg/etc/"]
       :source-paths  ["dev"]
       :set-version {
          :updates [{:path "src/re-mote/launch.clj" :search-regex #"\"re-mote \d+\.\d+\.\d+\""}]
       }
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


  :repl-options {
    :init-ns user
  }

  :aliases {
     "autotest" ["midje" ":autotest" ":filter" "-integration"]
     "runtest" ["midje" ":filter" "-integration"]
     "start-repl" ["do" "clean," "cljsbuild" "once," "repl" ":headless"]
     "start" ["do" "clean," "cljsbuild" "once," "run"]
   }

  ;; :aot [re-mote.repl.base]

  :target-path "target/"

  :signing {:gpg-key "narkisr@gmail.com"}

  :main supernal.launch
  )
