# Intro

A live coding environment for remote operations.

[![Build Status](https://travis-ci.org/re-ops/re-mote.png)](https://travis-ci.org/re-ops/re-ops)

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

# Prerequisite

* JDK 8 with JCE enabled (see below on how to install JCE).
* lein (see https://leiningen.org/).
* rng-tools for increased entropy under Linux (Ubuntu).
* A solid understanding of Clojure :)

## Setting JCE

```bash
# Download http://www.oracle.com/technetwork/java/javase/downloads/jce-7-download-432124.html
$ unzip UnlimitedJCEPolicyJDK7.zip
$ sudo cp UnlimitedJCEPolicy/* /usr/lib/jvm/java-7-oracle/jre/lib/security/
```

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
