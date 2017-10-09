(ns re-mote.zero.send
  "Sending back responses to the agents"
  (:require
    [clojure.core.strint :refer  (<<)]
    [taoensso.timbre :refer  (refer-timbre)]
    [taoensso.nippy :as nippy :refer (freeze thaw)]
    [re-share.zero.common :refer (close)])
  (:import
    [org.zeromq ZMQ ZMsg]))

(refer-timbre)

(defn send-push [ctx]
  (doto (.socket ctx ZMQ/PUSH)
    (.setLinger 0)
    (.connect "inproc://send-in")))

(defn control-sub-socket [ctx]
  (doto (.socket ctx ZMQ/SUB)
    (.setLinger 0)
    (.subscribe ZMQ/SUBSCRIPTION_ALL)
    (.connect "inproc://sndc")))

(defn control-pub-socket [ctx]
  (doto (.socket ctx ZMQ/PUB)
    (.setLinger 0)
    (.bind "inproc://sndc")))

(defn inproc [ctx end t]
  (doto
    (.socket ctx t)
    (.setLinger 0)
    (.bind (<< "inproc://~{end}"))))

(def pool (ref []))

(defn take- [pool]
  (dosync
    (let [[h & t] @pool]
      (ref-set pool (vec t))
      h)))

(defn put [pool x]
  (dosync
    (alter pool conj x)
    nil))

(defn start [ctx]
  (future
    (dotimes [i 1]
      (put pool (send-push ctx))) 
    (let [rcv (inproc ctx "send-in" ZMQ/PULL) 
          snd (inproc ctx "send-out" ZMQ/PUSH)
          control (control-sub-socket ctx)]
      (ZMQ/proxy rcv snd nil control)
      (close rcv)
      (close snd)
      (close control)
      (info "send closed"))))

(defn stop [ctx]
  (when @pool 
    (info "clearing pool")
    (dosync
      (doseq [push @pool]
        (close push))
      (ref-set pool [])))
  (let [control (control-pub-socket ctx)]
    (info (.send control "TERMINATE" 0))
    (close control)
    (info "send shutdown called")))
