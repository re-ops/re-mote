# Intro

A live coding environment for remote operations.

[![Build Status](https://travis-ci.org/re-ops/re-mote.png)](https://travis-ci.org/re-ops/re-mote)

## Motivation

Most (if not all) configuration management tools currently are static in nature, you write you code deploy it and apply (rinse and repeat).

Still our live environments change rapidly and having this cycle in place really slows our reaction time down.

RE-mote is a re-take on how remote operations would look like when using a live REPL to drive them.

# Get running

```clojure
$ lein repl
user=> (use 're-mote.repl)
nil
user=> (go)

```

Now we can start and play (see src/re_mote/repl.clj):

```clojure
; see https://github.com/opskeleton/supernal-sandbox
(def sandbox (Hosts. {:user "vagrant"} ["192.168.2.25" "192.168.2.26" "192.168.2.27"]))

; a simple list action on all hosts
(defn listing [hs]
  (run (ls hs "/" "-la") | (pretty)))

(listing sandbox)

; publish cpu/ram use to a dashboard
(defn stats [hs]
  (run (free hs) | (pretty) | (collect) | (publish (stock "Free RAM" :timeseries :free)) | (publish (stock "Used RAM" :timeseries :used)))
  (run (cpu hs)  | (pretty) | (collect) | (publish (stock "Idle CPU" :timeseries :idle)) | (publish (stock "User CPU" :timeseries :usr))))

(stats sandbox)
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
