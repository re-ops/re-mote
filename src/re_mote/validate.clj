(ns re-mote.validate
  (:require
   [clojure.string :refer (trim)])
  (:import
   [javax.crypto Cipher]))

(defn- read-entropy []
  (Integer/parseInt (trim (slurp "/proc/sys/kernel/random/entropy_avail"))))

(defn check-entropy [limit]
  (let [entropy (read-entropy)]
    (when (and (= "Linux" (System/getProperty "os.name")) (< entropy limit))
      (throw (ex-info "Available entropy is too low" {:available entropy :limit limit})))))

(defn check-jce []
  (let [allowed (Cipher/getMaxAllowedKeyLength "AES")]
    (when (> Integer/MAX_VALUE allowed)
      (throw (ex-info "JCE isn't available" {:max-allowed allowed})))))

