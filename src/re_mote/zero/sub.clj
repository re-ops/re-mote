(ns re-mote.zero.sub
 (:require
   [re-mote.zero.common :refer (read-key context)])
 (:import
    [java.nio.charset Charset]
    [org.zeromq ZMQ]
    [java.nio.charset Charset]))

(defn sub-socket [host {:keys [client-prv client-pub server-pub]}]
  (doto
    (.socket (context) ZMQ/SUB)
    (.setCurveServerKey (read-key ".curve/server-public.key"))
    (.setCurvePublicKey (read-key ".curve/client-public.key"))
    (.setCurveSecretKey  (read-key ".curve/client-private.key"))
    (.connect "tcp://127.0.0.1:9000")))

(defn read-loop [topic]
   (let [client (sub-socket)]
      (.subscribe client (.getBytes topic));
      (loop []
        (println "waiting")
        (println (.recvStr client 0 (Charset/defaultCharset)))
        (recur))))

;; (read-loop "play")
