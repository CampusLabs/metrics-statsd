# metrics-statsd

A batching [StatsD] reporter for [Coda Hale's metrics library] along
with a simple StatsD client and server library.

## Rationale

There are many implementations of the StatsD protocol, but most send a
packet per metric. This is fine for a few metrics but adds considerable
overhead for applications with dozens or hundreds of metrics. We found
one reporter that aggregates all metrics from a report into a single
packet with a fixed buffer size, but there was nothing to prevent that
buffer from being exceeded, causing metrics to be dropped and corrupted.

This library provides a metrics reporter that aggregates metrics into
batches based on a maximum size (in bytes) and a maximum latency,
sending the minimum number of packets while still maintaining the MTU
requirements of your network.

## Usage

### Dependencies

Leiningen:
```
[metrics-statsd "0.1.2"]
```

Maven:
```xml
<dependency>
  <groupId>metrics-statsd</groupId>
  <artifactId>metrics-statsd</artifactId>
  <version>0.1.2</version>
</dependency>
```

Maven repository:
```xml
<repository>
  <id>clojars.org</id>
  <url>http://clojars.org/repo</url>
</repository>
```

### Metrics Reporter

#### Java

The metrics-statsd reporter is exposed as a Java class:
```java
final MetricRegistry metrics = new MetricRegistry();

// create a StatsD reporter that sends metrics to localhost
StatsDReporter reporter = new StatsDReporter(registry, "127.0.0.1");

// start reporting metrics every 15 seconds
reporter.start(15, TimeUnit.SECONDS);
```

A longer constructor is also provided which takes the following
parameters:

| parameter | type | library default | description |
|-----------|------|---------|-------------|
| `registry` | MetricRegistry | required | report source |
| `host` | String | "127.0.0.1" | StatsD host |
| `port` | Integer | 8125 | StatsD port |
| `maxSize` | Integer | 1432 | maximum metric payload in bytes |
| `maxLatency` | Integer | 20 | maximum batch latency |
| `filter` | MetricFilter | allow all | filter metrics to report |
| `rateUnit` | TimeUnit | seconds | time unit for rates |
| `durationUnit` | TimeUnit | milliseconds | time unit for durations |

#### Clojure

A Clojure function is also provided to generate a new reporter:
```clojure
(require '[metrics-statsd.core :as statsd])
(import 'com.codahale.metrics.MetricRegistry
        'java.util.concurrent.TimeUnit)

(def registry (MetricRegistry.))

;; create a new metrics reporter with default options
(def reporter (statsd/new-statsd-reporter registry))

;; start reporting metrics every 15 seconds
(.start reporter 15 TimeUnit/SECONDS)
```

`new-statsd-reporter` can take an additional options map corresponding
to the `StatsDReporter` constructor parameters mentioned above:
```clojure
(statsd/new-statsd-reporter
  {:host          "127.0.0.1"
   :port          8125
   :max-size      1432
   :max-latency   20
   :filter        MetricFilter/ALL
   :rate-unit     TimeUnit/SECONDS
   :duration-unit TimeUnit/MILLISECONDS})
```

### StatsD Client

You can use metrics-statsd's batching statsd client independently from
Coda Hale's metrics library. It provides a [Manifold stream] that
sends any metrics put on the stream. Metrics are maps with the following
keys:

| key | type | description |
|-----|------|-------------|
| `:name` | string | metric name; `:`, `|`, and `@` are replaced with `_` |
| `:type` | symbol | `:gauge`, `:counter`, `:timer`, `:histogram`, `:meter`, or `:set` |
| `:value` | double |metric value; all numbers coerced to doubles |

To send metrics, `put!` metrics onto the client, which returns a
[Manifold deferred]:

```clojure
(require '[manifold.stream :as s]
         '[metrics-statsd.communication :as comm])

;; create a statsd client for:
;;   server:            127.0.0.1
;;   port:              8125
;;   max batch size:    1432 (bytes)
;;   max batch latency: 20 (milliseconds)
(def client (comm/client "127.0.0.1" 8125 1432 20))

;; send a counter metric
@(s/put! client {:name  "test.counter"
                 :type  :counter
                 :value 0})
;; => true

;; close the client socket:
(s/close! client)
```

Metrics will be batched into payloads of max size within max latency and
sent to the server.

### StatsD Server

metrics-statsd also provides a server which understands batches.
`server` returns a [Manifold stream] source containing all metrics
sent from clients.

```clojure
;; create a server that listens on port 8125
(def server (comm/server 8125))

;; whenever a new metric arrives, print it
(s/consume #(println %) server)

;; send three metrics from the client
(dotimes [n 3]
  @(s/put! client {:name  (str "test" n ".counter")
                   :type  :counter
                   :value n}))

;; the following lines are printed:
;; {:name test0.counter, :value 0.0, :type :counter}
;; {:name test1.counter, :value 1.0, :type :counter}
;; {:name test2.counter, :value 2.0, :type :counter}

;; close the server socket:
(s/close! server)
```

## License

Copyright 2016 OrgSync.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

[StatsD]: https://github.com/b/statsd_spec
[Coda Hale's metrics library]: http://metrics.dropwizard.io
[Manifold]: http://aleph.io/manifold/rationale.html
[Manifold stream]: http://aleph.io/manifold/streams.html
[Manifold deferred]: http://aleph.io/manifold/deferreds.html
