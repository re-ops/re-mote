(ns re-mote.zero.send
  "Queue for sending back responses to the agents"
  (:require
   [taoensso.timbre :refer  (refer-timbre)])
  (:import
   [java.util.concurrent TimeUnit]
   [org.zeromq ZMQ ZMsg]))

(refer-timbre)

(def maxx 1000)

(def queue (java.util.concurrent.LinkedBlockingQueue. maxx))

(defn take- []
  (.poll queue 10 TimeUnit/MILLISECONDS))

(defn send- [address content]
  (.offer queue [address content] 100 TimeUnit/MILLISECONDS))

(defn start [])

(defn stop []
  (.clear queue))

