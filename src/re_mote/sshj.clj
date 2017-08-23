(ns re-mote.sshj
  (:require
   [me.raynes.conch :as c]
   [clojure.java.io :as io :refer (reader output-stream)]
   [clojure.string :refer (join split)]
   [clojure.java.shell :refer [sh]]
   [clojure.core.strint :refer (<<)]
   [taoensso.timbre :refer (refer-timbre)]
   [re-mote.log :refer (log-output)]
   [clojure.string :refer (split)])
  (:import
   clojure.lang.ExceptionInfo
   (java.util.concurrent TimeUnit)
   (net.schmizz.sshj.common StreamCopier$Listener)
   (net.schmizz.sshj.xfer FileSystemFile TransferListener)
   (net.schmizz.sshj SSHClient)
   (net.schmizz.sshj.userauth.keyprovider FileKeyProvider)
   (net.schmizz.sshj.transport.verification PromiscuousVerifier)))

(refer-timbre)

(def default-key (<< "~(System/getProperty \"user.home\")/.ssh/id_rsa"))
(def default-user "root")
(def default-port 22)

(def ^:dynamic timeout (* 1000 60 10))

(defn ssh-strap [{:keys [host ssh-port ssh-key user]}]
  (doto (SSHClient.)
    (.addHostKeyVerifier (PromiscuousVerifier.))
    (.setTimeout timeout)
    (.connect host (or ssh-port default-port))
    (.authPublickey user #^"[Ljava.lang.String;" (into-array [(or ssh-key default-key)]))))

(defmacro with-ssh [remote & body]
  `(let [~'ssh (ssh-strap ~remote)]
     (try
       ~@body
       (catch Throwable e#
         (throw e#))
       (finally
         (trace "disconneted ssh")
         (.disconnect ~'ssh)))))

(defn execute
  "Executes a cmd on a remote host"
  [cmd remote & {:keys [out-fn err-fn] :or {out-fn log-output err-fn log-output}}]
  (with-ssh remote
    (let [session (doto (.startSession ssh) (.allocateDefaultPTY)) command (.exec session cmd)]
      (try (debug (<< "[~(remote :host)]:") cmd)
           (out-fn (.getInputStream command) (remote :host))
           (err-fn (.getErrorStream command) (remote :host))
           (.join command 60 TimeUnit/SECONDS)
           (.getExitStatus command)
           (finally
             (.close session)
             (trace "session closed!"))))))

(def listener
  (proxy [TransferListener] []
    (directory [name*] (debug "starting to transfer" name*))
    (file [name* size]
      (proxy [StreamCopier$Listener] []
        (reportProgress [transferred]
          (debug (<< "transferred ~(int (/ (* transferred 100) size))% of ~{name*}")))))))

(defn upload
  [src dst remote]
  (when-not (.exists (io/file src))
    (throw (ex-info "missing source file" {:file src})))
  (with-ssh remote
    (let [scp (.newSCPFileTransfer ssh)]
      (.setTransferListener scp listener)
      (.upload scp (FileSystemFile. src) dst))))

(defn ssh-up? [remote]
  (try
    (with-ssh remote (.isConnected ssh))
    (catch java.net.ConnectException e false)))

(defn fname [uri] (-> uri (split '#"/") last))

(defn- options [args]
  (let [log-proc (fn [out proc] (info out))
        defaults {:verbose true :timeout (* 60 1000) :out log-proc :err log-proc}]
    (if (map? (last args))
      [(butlast args) (merge defaults (last args))]
      [args defaults])))

(defn sh-
  "Runs a command localy and logs its output streams"
  [cmd & args]
  (let [[args opts] (options args)]
    (info cmd (join " " args))
    (case (deref (:exit-code (c/run-command cmd args opts)))
      :timeout (throw (ExceptionInfo. (<< "timed out while executing: ~{cmd}") opts))
      0 nil
      (throw (ExceptionInfo. (<< "Failed to execute: ~{cmd}") opts)))))
