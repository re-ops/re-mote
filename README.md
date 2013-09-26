# Intro

A remote multi server automation tool (Capistrano/Fabric) in Clojure.


[![Build Status](https://travis-ci.org/celestial-ops/supernal.png)](https://travis-ci.org/celestial-ops/supernal)

# Goals and motivation
 
 * A clear roles to host matching model which can be extended easily.
 
 * Can be used both as a library and as a standalone tool.

 * An easy to follow Clojure DSL. 

 * Functional interface.

 * Inspired by Capistrano but more flexible.


# Usage

Code deployment example, first we define our tasks:

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

Then we define the lifecycle (how they relate to each other):

```clojure
(lifecycle base-deploy
  {deploy/update-code #{deploy/post-update deploy/symlink}
   deploy/stop #{deploy/update-code}
   deploy/pre-update #{deploy/update-code}
   deploy/symlink #{deploy/start} 
   deploy/post-update #{deploy/start}
   deploy/start #{}})
```

Execution and environment:

```clojure
(env 
  {:roles {
      :web #{{:host "foobar" :user "vagrant" :sudo true}}}
   })

(def artifact "git://github.com/narkisr/swag.git")

(execute base-deploy {:app-name "foo" :src artifact} :web)
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
