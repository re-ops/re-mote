(comment 
  Celestial, Copyright 2012 Ronen Narkis, narkisr.com
  Licensed under the Apache License,
  Version 2.0  (the "License") you may not use this file except in compliance with the License.
  You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.)

(ns supernal.sshj
  (:use 
    [clojure.java.io :only (reader output-stream)]
    [clojure.string :only (join split)]
    [clojure.java.shell :only [sh]]
    [clojure.core.strint :only (<<)]
    [taoensso.timbre :only (warn debug info error)]
    [clojure.string :only (split)]
    [plumbing.core :only (defnk)] 
    ) 
  (:import 
    (java.util.concurrent TimeUnit)
    (net.schmizz.sshj.common StreamCopier$Listener)
    (net.schmizz.sshj.xfer FileSystemFile TransferListener)
    (net.schmizz.sshj SSHClient)
    (net.schmizz.sshj.userauth.keyprovider FileKeyProvider)
    (net.schmizz.sshj.transport.verification PromiscuousVerifier)
    )
   (:require [aws.sdk.s3 :as s3]) 
  )

(defn default-config []
  {:key (<< "~(System/getProperty \"user.home\")/.ssh/id_rsa" ) :user "root" })

(def config (atom (default-config)))

(defn log-output 
  "Output log stream" 
  [out host]
  (doseq [line (line-seq (reader out))] (debug  (<< "[~{host}]:") line)))

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
  [cmd remote]
    (with-ssh remote 
      (let [session (doto (.startSession ssh) (.allocateDefaultPTY)) command (.exec session cmd) ]
          (debug (<< "[~(remote :host)]:") cmd) 
          (log-output (.getInputStream command) (remote :host))
          (log-output (.getErrorStream command) (remote :host))
          (.join command 60 TimeUnit/SECONDS) 
          (when-not (= 0 (.getExitStatus command))
            (throw (Exception. (<< "Failed to execute ~{cmd} on ~{remote}")))))))

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

(defmethod copy-remote :git [uri dest remote] 
  (execute (<< "git clone ~{uri} ~(dest-path uri dest)") remote))
(defmethod copy-remote :http [uri dest remote] 
  (execute (<< "wget -O ~(dest-path uri dest) ~{uri}") remote))
(defmethod copy-remote :s3 [uri dest remote] 
  (execute (<< "s3cmd get ~{uri} ~(dest-path uri dest)") remote))
(defmethod copy-remote :file [uri dest remote] (upload (subs uri 6) dest remote))
(defmethod copy-remote :default [uri dest remote] (copy-remote (<< "file:/~{uri}") dest remote))

(defn log-res 
  "Logs a cmd result"
  [out]
  (when-not (empty? out) 
    (doseq [line (.split out "\n")] (info line))))

(defn sh- 
  "Runs a command localy and logs it "
  [& cmds]
  (let [{:keys [out err exit]} (apply sh cmds) cmd (join " " cmds)]
    (info cmd)
    (log-res out) 
    (log-res err) 
    (when-not (= 0 exit) 
      (throw (Exception. (<< "Failed to execute: ~{cmd}"))))))

(def ^:dynamic s3-creds  {:access-key "" :secret-key ""})

(defn s3-copy [bucket k dest]
  (let [{:keys [content]} (s3/get-object s3-creds bucket k)]
    (with-open [w (output-stream (<< "~{dest}/~{k}"))]
      (clojure.java.io/copy content w))))

(defmulti copy-localy
  "A general local copy"
  copy-dispatch)

(defmethod copy-localy :git [uri dest] 
  (sh- "git" "clone" uri  (<< "~{dest}/~(no-ext (fname uri))")))
(defmethod copy-localy :http [uri dest] 
  (sh- "wget" "-O" (<< "~{dest}/~(fname uri) ~{uri}")))
(defmethod copy-localy :s3 [uri dest] 
  (let [[_ bucket k] (re-find s3-regex)] (s3-copy bucket k dest)))
(defmethod copy-localy :file [uri dest] (sh- "cp" (subs uri 6) dest))
(defmethod copy-localy :default [uri dest] (copy-localy (<< "file:/~{uri}") dest))

(defn copy 
  "A general copy utility for both remote and local uri's http/git/file protocols are supported
  assumes a posix system with wget/git, for remote requires key based ssh access."
  ([uri dest] (copy-localy uri dest)) 
  ([uri dest remote] (copy-remote uri dest remote)))

(test #'no-ext)
; (execute "ping -c 1 google.com" {:host "localhost" :user "ronen"}) 
; (upload "/home/ronen/Downloads/PCBSD9.1-x64-DVD.iso" "/tmp" {:host "localhost" :user "ronen"})
