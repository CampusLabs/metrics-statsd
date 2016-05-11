;; Copyright 2016 OrgSync
;;
;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at
;;
;; http://www.apache.org/licenses/LICENSE-2.0
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.

(defproject metrics-statsd "0.1.4"
  :description "A batching StatsD reporter for Coda Hale's metrics library"
  :url "https://github.com/orgsync/metrics-statsd"
  :license {:name "Apache 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[aleph "0.4.1"]
                 [byte-streams "0.2.2"]
                 [gloss "0.2.6"]
                 [io.dropwizard.metrics/metrics-core "3.1.2"]
                 [manifold "0.1.4"]
                 [org.clojure/clojure "1.8.0"]]
  :aot [metrics-statsd.reporter]
  :profiles {:dev {:dependencies [[criterium "0.4.4"]]}}
  :main metrics-statsd.reporter.StatsDReporter)
