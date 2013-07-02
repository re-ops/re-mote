(ns supernal.test.sshj
  (:use 
    midje.sweet
    supernal.sshj
    )
 )

(fact "git resolving uri's"
   (copy-dispatch "git://github.com/celestial-ops/celestial-core.git") => :git
   (copy-dispatch "git@github.com:celestial-ops/celestial-core.git") => :git
   (copy-dispatch "https://github.com/celestial-ops/celestial-core.git") => :git
   (copy-dispatch "http://dl.bintray.com/narkisr/boxes/celestial-sandbox-0.1.0.tar.gz?direct") => :http
   (copy-dispatch "s3://logix.cz-test/addrbook.xml") => :s3
   (copy-dispatch "https://dl.bintray.com/narkisr/boxes/celestial-sandbox-0.1.0.tar.gz?direct") => :http
   (copy-dispatch "file://tmp/celestial-sandbox-0.1.0.tar.gz?direct") => :file)
