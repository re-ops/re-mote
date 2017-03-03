(comment
  Celestial, Copyright 2017 Ronen Narkis, narkisr.com
  Licensed under the Apache License,
  Version 2.0  (the "License") you may not use this file except in compliance with the License.
  You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.)

(ns supernal.sshj
  (:require
    [me.raynes.conch :as c]
    [aws.sdk.s3 :as s3]
    [clojure.java.io :refer (reader output-stream)]
    [clojure.string :refer (join split)]
    [clojure.java.shell :refer [sh]]
    [clojure.core.strint :refer (<<)]
    [taoensso.timbre :refer (warn debug info error)]
    [clojure.string :refer (split)]
    [plumbing.core :refer (defnk)])
  (:import
    clojure.lang.ExceptionInfo
    (java.util.concurrent TimeUnit)
    (net.schmizz.sshj.common StreamCopier$Listener)
    (net.schmizz.sshj.xfer FileSystemFile TransferListener)
    (net.schmizz.sshj SSHClient)
    (net.schmizz.sshj.userauth.keyprovider FileKeyProvider)
    (net.schmizz.sshj.transport.verification PromiscuousVerifier)))

(defn default-config []
  {:key (<< "~(System/getProperty \"user.home\")/.ssh/id_rsa" ) :user "root" })

(def config (atom (default-config)))

(defn log-output
  "Output log stream"
  [out host]
  (doseq [line (line-seq (reader out))] (debug  (<< "[~{host}]:") line)))

(def logs (atom {}))

(defn collect-log
  "Collect log output into logs atom"
  [uuid]
   (fn [out host]
     (swap! logs (fn [m] (assoc m uuid (doall (map (fn [line] (info  (<< "[~{host}]:") line) line) (line-seq (reader out)))))))))

(defn get-log
  "Getting log entry and clearing it"
  [uuid]
   (when-let [res (get @logs uuid)]
      (swap! logs (fn [m] (dissoc m uuid)))
      res
     ))

(def ^:dynamic timeout (* 1000 60 10))

(defnk ssh-strap [host {user (@config :user)} {ssh-port 22}]
  (doto (SSHClient.)
    (.addHostKeyVerifier (PromiscuousVerifier.))
    (.setTimeout timeout)
    (.connect host ssh-port)
    (.authPublickey user #^"[Ljava.lang.String;" (into-array [(@config :key)]))))

(defmacro with-ssh [remote & body]
  `(let [~'ssh (ssh-strap ~remote)]
     (try
       ~@body
       (catch Throwable e#
         (.disconnect ~'ssh)
         (throw e#)
         ))))

(defn execute
  "Executes a cmd on a remote host"
  [cmd remote & {:keys [out-fn err-fn] :or {out-fn log-output err-fn log-output}}]
  (with-ssh remote
    (let [session (doto (.startSession ssh) (.allocateDefaultPTY)) command (.exec session cmd) ]
      (debug (<< "[~(remote :host)]:") cmd)
      (out-fn (.getInputStream command) (remote :host))
      (err-fn (.getErrorStream command) (remote :host))
      (.join command 60 TimeUnit/SECONDS)
      (.getExitStatus command))))

(def listener
  (proxy [TransferListener] []
    (directory [name*] (debug "starting to transfer" name*))
    (file [name* size]
      (proxy [StreamCopier$Listener ] []
        (reportProgress [transferred]
          (debug (<< "transferred ~(int (/ (* transferred 100) size))% of ~{name*}")))))))

(defn upload [src dst remote]
  (with-ssh remote
    (let [scp (.newSCPFileTransfer ssh)]
      (.setTransferListener scp listener)
      (.upload scp (FileSystemFile. src) dst)
      )))

(defn ssh-up? [remote]
  (try
    (with-ssh remote (.isConnected ssh))
    (catch java.net.ConnectException e false)))

(defn fname [uri] (-> uri (split '#"/") last))

(defn ^{:test #(assert (= (no-ext "celestial.git") "celestial"))}
  no-ext
  "file name without extension"
  [name]
  (-> name (split '#"\.") first))

(def s3-regex #"^s3:\/\/(.*)\/(.*)")

(def classifiers
  [[:git #{#(re-find #".*.git$" %)}]
   [:s3 #{#(re-find s3-regex %)}]
   [:http #{#(re-find #"^(http|https)" %)}]
   [:file #{#(re-find #"^file.*" %)}]])


(defn copy-dispatch
  ([uri _ _ _] (copy-dispatch uri))
  ([uri _ _] (copy-dispatch uri))
  ([uri _] (copy-dispatch uri))
  ([uri] {:pre [uri]}
   (first (first (filter #(some (fn [c] (c uri) ) (second %)) classifiers )))))

(defmulti dest-path
  "Calculates a uri destination path"
  copy-dispatch)

(defmethod dest-path :git [uri dest] (<< "~{dest}/~(no-ext (fname uri))"))
(defmethod dest-path :http [uri dest] (<< "~{dest}/~(fname uri) ~{uri}"))
(defmethod dest-path :default [uri dest] dest)

(defmulti copy-remote
  "A general remote copy"
  copy-dispatch
  )

(defn wget-options [{:keys [unsecure]}] (if unsecure "--no-check-certificate" ""))

(defmethod copy-remote :git [uri dest opts remote]
  (execute (<< "git clone ~{uri} ~(dest-path uri dest)") remote))
(defmethod copy-remote :http [uri dest opts remote]
  (execute (<< "wget ~(wget-options opts) -O ~(dest-path uri dest) ~{uri}") remote))
(defmethod copy-remote :s3 [uri dest opts remote]
  (execute (<< "s3cmd get ~{uri} ~(dest-path uri dest)") remote))
(defmethod copy-remote :file [uri dest opts remote] (upload (subs uri 6) dest remote))
(defmethod copy-remote :default [uri dest opts remote] (copy-remote (<< "file:/~{uri}") dest opts remote))

(defn log-res
  "Logs a cmd result"
  [out]
  (when-not (empty? out)
    (doseq [line (.split out "\n")] (info line))))

(defn- options [args]
  (let [log-proc (fn [out proc] (info out))
        defaults {:verbose true :timeout (* 60 1000) :out log-proc :err log-proc}]
    (if (map? (last args))
      [(butlast args) (merge defaults (last args))]
      [args defaults])))

(defn sh-
  "Runs a command localy and logs its output streams"
  [cmd & args]
  (let [[args opts] (options args) ]
    (info cmd (join " " args))
    (case (deref (:exit-code (c/run-command cmd args opts)))
      :timeout (throw (ExceptionInfo. (<< "timed out while executing: ~{cmd}") opts))
      0 nil
      (throw (ExceptionInfo. (<< "Failed to execute: ~{cmd}") opts)))))

(def ^:dynamic s3-creds  {:access-key "" :secret-key ""})

(defn s3-copy [bucket k dest]
  (let [{:keys [content]} (s3/get-object s3-creds bucket k)]
    (with-open [w (output-stream (<< "~{dest}/~{k}"))]
      (clojure.java.io/copy content w))))

(defmulti copy-localy
  "A general local copy"
  copy-dispatch)

(defmethod copy-localy :git [uri dest opts]
  (sh- "git" "clone" uri  (<< "~{dest}/~(no-ext (fname uri))")))
(defmethod copy-localy :http [uri dest opts]
  (sh- "wget" "--no-check-certificate" "-O" (<< "~{dest}/~(fname uri) ~{uri}")))
(defmethod copy-localy :s3 [uri dest opts]
  (let [[_ bucket k] (re-find s3-regex)] (s3-copy bucket k dest)))
(defmethod copy-localy :file [uri dest opts] (sh- "cp" (subs uri 6) dest))
(defmethod copy-localy :default [uri dest opts] (copy-localy (<< "file:/~{uri}") dest {}))

(defn copy
  "A general copy utility for both remote and local uri's http/git/file protocols are supported
  assumes a posix system with wget/git, for remote requires key based ssh access."
  ([uri dest opts] (copy-localy uri dest opts))
  ([uri dest opts remote] (copy-remote uri dest opts remote)))

(test #'no-ext)

(defn gen-uuid [] (.replace (str (java.util.UUID/randomUUID)) "-" ""))
