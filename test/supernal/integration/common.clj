(ns supernal.integration.common)

(defn base-net [suffix]
  (str (or (System/getenv "BASE_NET") "192.168.2") suffix))
