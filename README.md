# Intro

A live coding environment for configuration management.

[![Build Status](https://travis-ci.org/re-ops/re-mote.png)](https://travis-ci.org/re-ops/re-mote)

## Motivation

Most (if not all) configuration management tools currently are static in nature, you write you code deploy it and apply (rinse and repeat).

Still our live environments change rapidly and having this cycle in place really slows our reaction time down.

RE-mote is a re-take on how remote operations would look like when using a live REPL to drive them.

# Get running

```clojure
$ git clone git@github.com:re-ops/re-mote.git
$ cd re-mote
$ lein repl
user=> (go)
nil
user=> (listing sandbox)

Run summary:

   ✔ 192.168.2.28
   ✔ 192.168.2.27
   ✔ 192.168.2.26

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


We can publish results to a dashboard (check http://host:8080):
```clojure
(defn cpu-publish [hs]
 (run (cpu hs)  | (collect) | (publish (stock "Idle CPU" :timeseries :idle)) | (publish (stock "User CPU" :timeseries :usr))))
```

And scehdule them:

```clojure
; every 5 seconds, check re-mote.repl.schedule for more options
(defn stats-jobs [hs]
  (watch :ram (seconds 5) (fn [] (ram-publish hs)))
  (watch :net (seconds 5) (fn [] (net-publish hs)))
  (watch :cpu (seconds 5) (fn [] (cpu-publish hs)))
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

# Prerequisite

* JDK 8 with JCE enabled (On Ubuntu oracle-java8-unlimited-jce-policy using PPA).
* lein (see https://leiningen.org/).
* rng-tools for increased entropy under Linux (Ubuntu).
* A solid understanding of Clojure :)

# Copyright and license

Copyright [2017] [Ronen Narkis]

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
