(ns re-mote.zero.keys
 (:import
   [org.zeromq ZCert ZContext ZAuth]))

(defn setup
  "Setup auth context"
  []
  (doto (ZAuth. (ZContext.))
   (.setVerbose true)
   (.allow "127.0.0.1")
   (.configureCurve ".curve")))

(defn generate-pair
  "Generate pub/secret key pairs"
  [parent prefix]
  (let [zcert (ZCert.)]
    (spit (str parent "/" prefix "-private.key") (.getSecretKeyAsZ85 zcert) )
    (spit (str parent "/" prefix "-public.key") (.getPublicKeyAsZ85 zcert) )))

(defn create-keys 
   "Create both client and server keys" 
   [parent]
  (generate-pair parent "client")
  (generate-pair parent "server"))


(comment 
  (create-keys ".curve") 
  )
