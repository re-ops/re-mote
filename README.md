# Intro

A remote multi server automation tool (Capistrano/Fabric) in Clojure.


[![Build Status](https://travis-ci.org/celestial-ops/supernal.png)](https://travis-ci.org/celestial-ops/supernal)

# Goals and motivation
 
 * A clear roles to host matching model which can be extended easily.
 
 * Can be used both as a library and as a standalone tool.

 * An easy to follow Clojure DSL. 

 * Functional interface.

 * Inspired by Capistrano but more flexible.

# Install 

```bash 
$ wget -qO - http://bit.ly/barbecue-ops  | sudo apt-key add -
$ sudo add-apt-repository 'deb http://celestial-ops.com/barbecue quantal main'
$ sudo apt-get update && sudo apt-get install supernal
```

Make sure to enable JCE:

```bash
# Download http://www.oracle.com/technetwork/java/javase/downloads/jce-7-download-432124.html
$ unzip UnlimitedJCEPolicyJDK7.zip
$ sudo cp UnlimitedJCEPolicy/* /usr/lib/jvm/java-7-oracle/jre/lib/security/
```

# Usage

We will follow a basic code deployment scenario, note that supernal uses SSH under to hood and expects the running user ssh key to be autorized on the remote machine end (see ssh-copy-id and authorized file).

## Defining tasks

We define tasks using a Clojure DSL where each task is enclosed within a namespace:

```clojure
; baseline.clj

(ns- deploy 
  (task update-code
    (let [{:keys [src app-name run-id]} args]
      (debug "updating code on" remote) 
      (copy src (releases app-name run-id)))) 
 
  (task post-update
    (let [{:keys [src app-name run-id]} args file (last (split src #"/")) 
          basepath (releases app-name run-id)]
      (when-let [ext (archive? file)]
        (debug ext)
        (run ((archive-types (<< "~{basepath}~{file}") basepath) ext)))))

  (task start 
    (debug "starting service on" remote)) 
 
  (task symlink
    (let [{:keys [app-name run-id]} args]
      (run (<< "rm -f ~(current app-name)"))
      (run (<< "ln -s ~(releases app-name run-id) ~(current app-name)"))))

  (task stop
    (debug "stopping service on" remote))
     
  (task pre-update
    (let [{:keys [app-name run-id]} args release-id (date-fmt)]
      (swap! run-ids assoc run-id release-id)
      (run (<< "mkdir ~(releases app-name run-id) -p"))
      (run (<< "chown ~(remote :user) ~(releases app-name run-id)"))))) 
```

We define how the task relate to each other using a lifecycle definition (basicly a graph):

```clojure
(lifecycle base-deploy
  {deploy/update-code #{deploy/post-update deploy/symlink}
   deploy/stop #{deploy/update-code}
   deploy/pre-update #{deploy/update-code}
   deploy/symlink #{deploy/start} 
   deploy/post-update #{deploy/start}
   deploy/start #{}})
```

Lastly we define our running enviroment which includes the mapping from roles to specific hosts:

```clojure
(env 
  {:roles {
      :web #{{:host "foobar" :user "vagrant" :sudo true}}}
   })
```

## Launch
We can either launch it using programatic api:

```clojure
(def artifact "git://github.com/narkisr/swag.git")

(execute base-deploy {:app-name "foo" :src artifact} :web)
```

Or using the sup binary:

```bash
$ sup run -s fixtures/deploy.clj base-deploy -r web
```

## Sup binary

The sup binary supports listing and running of single tasks/lifecycles:

```bash 
# using deploy.clj 
$ sup list 
Lifecycles:
  base-rollback - base deployment roleback
  base-deploy - base deployment scheme
  base-success - base deployment success
Tasks:
  deploy:
   stop - Stoping server foo
   pre-update - pre code update actions
   start - starts deployed service
   post-update - runs post code update actions
   symlink - links current version to current
   update-code - updates deployed code
```

sup will use deploy.clj by default we can specify a custom filename:

```bash
$ sup list -s foo.clj
```

Launching a complete lifecycle on a role:

```bash 
$ sup run deploy -r web 
```

We can also launch single tasks:

```bash
$ sup run deploy/stop -r web 
```

Passing args to task/lifecycle:

```bash
$ sup run {task/lifecycle} -r web  -a "src='foo',app-name='bla'"
```

# Copyright and license

Copyright [2013] [Ronen Narkis]

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
