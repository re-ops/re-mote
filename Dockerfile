FROM clojure
COPY . /usr/src/re-mote
WORKDIR /usr/src/re-mote
RUN wget https://raw.githubusercontent.com/narkisr/fpm-barbecue/repo/packages/ubuntu/pool/main/libz/libzmq-jni/libzmq-jni_3.1.1_amd64.deb
RUN wget https://raw.githubusercontent.com/narkisr/fpm-barbecue/repo/packages/ubuntu/pool/main/libz/libzmq1/libzmq1_4.1.4_amd64.deb
RUN wget https://raw.githubusercontent.com/narkisr/fpm-barbecue/repo/packages/ubuntu/pool/main/libs/libsodium/libsodium_1.0.8_amd64.deb
RUN dpkg -i libzmq-jni_3.1.1_amd64.deb
RUN dpkg -i libzmq1_4.1.4_amd64.deb
RUN dpkg -i libsodium_1.0.8_amd64.deb
CMD ["lein", "repl"]
