# Intro
Running re-mote using Docker is easy:

```bash
$ sudo docker build -t re-mote .
# Using the existing .m2 should save us some time
$ sudo docker run -it -v ~/.m2:/root/.m2 re-mote /usr/local/bin/lein repl
```
