(ns re-mote.repl.spec
  "Running spec on hosts"
  (:require
   [re-mote.ssh.pipeline :refer (map-async)]
   [clojure.core.strint :refer (<<)]
   [clojure.java.shell :refer [sh with-sh-dir]])
  (:import [re_mote.repl.base Hosts]))

(defprotocol Spec
  (spec
    [this src target]))

(defn- run-spec
  "Run serverspecs on host"
  [src target host]
  (with-sh-dir src
    (sh "rake"
        (<< "serverspec:~{target}")  (<< "TARGET_HOST=~{host}"))))

(defn serverspec [src target host]
  (try
    (let [{:keys [exit out err]} (run-spec src target host)]
      (if-not (= exit 0)
        {:host host :code exit :error {:out (if-not (empty? out) out err)}}
        {:host host :code exit :result {:out out :exit exit}}))
    (catch Throwable e
      {:host host :code -1 :error {:out (.getMessage e)}})))

(extend-type Hosts
  Spec
  (spec [{:keys [hosts] :as this} src target]
    (let [results (map-async (partial serverspec src target) hosts)
          grouped (group-by :code results)]
      [this {:hosts hosts :success (grouped 0) :failure (dissoc grouped 0)}])))

(defn refer-spec []
  (require '[re-mote.repl.spec :as spc]))

(comment
  (serverspec "192.168.122.223" "/home/ronen/code/boxes/base-sandbox" "backup"))
