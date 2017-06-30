FROM clojure
COPY . /usr/src/re-mote
WORKDIR /usr/src/re-mote
CMD ["lein", "repl"]
