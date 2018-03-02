# Intro

Re-mote is a REPL driven environment for performing remote operations using SSH and [Re-gent](https://github.com/re-ops/re-gent).

It is a part of the [Re-ops](https://re-ops.github.io/re-ops/) project that offers a live coding environment for configuration management.

[![Build Status](https://travis-ci.org/re-ops/re-mote.png)](https://travis-ci.org/re-ops/re-mote)

# Get running

```clojure
$ git clone git@github.com:re-ops/re-mote.git
$ cd re-mote
$ lein repl # or lein trampoline run -m rebel-readline.main
[re-mote]λ: (go)
nil
[re-mote]λ: (listing sandbox)

Run summary:

   ✔ 192.168.2.28
   ✔ 192.168.2.27
   ✔ 192.168.2.26

```

# Configure

The configuration file is pretty much self explanatory:

```bash
$ cp config/re-mote.edn ~/.re-mote.edn
```

Now we can start and play (see src/re_mote/repl.clj),  we define the hosts we work with (using key authentication):

```clojure
(def sandbox (Hosts. {:user "vagrant"} ["192.168.2.28" "192.168.2.26" "192.168.2.27"]))
```

We define pipelines (plain functions), the results of operations are threaded through:
```clojure
(defn listing [hs]
  (run (ls hs "/" "-la") | (pretty)))
```

An operation is a part of a protocol extending Hosts:

```clojure
; re-mote.repl.base
(defrecord Hosts [auth hosts]
  Shell
  (ls [this target flags]
    [this (run-hosts this (script ("ls" ~target ~flags)))])
```

It returns the hosts operated upon and the result, thus enabling pipelines.


We can persist results to Elasticsearch and view them in Kibana/Grafana:
```clojure
(defn #^{:category :stats} cpu-persist
  "CPU usage and idle stats collection and persistence"
  [hs]
  (run (cpu hs) | (enrich "cpu") | (persist)))
```

Schedule them:

```clojure
; every 5 seconds, check re-mote.repl.schedule for more options
(defn stats-jobs [hs]
  (watch :ram (seconds 5) (fn [] (ram-persist hs)))
  (watch :net (seconds 5) (fn [] (net-persist hs)))
  (watch :cpu (seconds 5) (fn [] (cpu-persist hs)))
  )
```

The expected workflow is to re-eval functions on the go, stop restart and refresh can be used for bigger changes:

```clojure
; we want to stop all components
user=> (stop)
; usually re-eval a single function is enough, refresh is re-load all!
user=> (refresh)
; back into action again, no restart!
user=> (go)
```

# Prerequisites

* JDK 8 with JCE enabled (On Ubuntu oracle-java8-unlimited-jce-policy using PPA).
* lein (see https://leiningen.org/).
* rng-tools for increased entropy under Linux (Ubuntu).
* A solid understanding of Clojure :)

Optionally: 

* Elasticsearch 5.6.x for persisting results and metrics
* Grafana for viewing metrics
* Kibana for viewing results and failures

Check [re-dock](https://github.com/re-ops/re-dock) for a quick docker setup of the above.

# Copyright and license

Copyright [2018] [Ronen Narkis]

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
