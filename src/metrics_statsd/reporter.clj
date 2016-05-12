(ns metrics-statsd.reporter
  (:require [manifold
             [deferred :as d]
             [stream :as s]]
            [metrics-statsd
             [communication :as comm]
             [serialization :refer [serialize]]])
  (:gen-class
    :name metrics_statsd.reporter.StatsDReporter
    :extends com.codahale.metrics.ScheduledReporter
    :constructors {[com.codahale.metrics.MetricRegistry
                    String]
                   [com.codahale.metrics.MetricRegistry
                    String
                    com.codahale.metrics.MetricFilter
                    java.util.concurrent.TimeUnit,
                    java.util.concurrent.TimeUnit]

                   [com.codahale.metrics.MetricRegistry
                    String
                    Integer
                    Integer
                    Integer
                    com.codahale.metrics.MetricFilter
                    java.util.concurrent.TimeUnit,
                    java.util.concurrent.TimeUnit]
                   [com.codahale.metrics.MetricRegistry
                    String
                    com.codahale.metrics.MetricFilter
                    java.util.concurrent.TimeUnit,
                    java.util.concurrent.TimeUnit]}
    :init init
    :state state
    :exposes-methods {start  parentStart
                      report parentReport
                      stop   parentStop})
  (:import com.codahale.metrics.MetricFilter
           java.util.concurrent.TimeUnit
           metrics_statsd.reporter.StatsDReporter
           [org.slf4j Logger LoggerFactory]))

(def default-host "127.0.0.1")
(def default-filter MetricFilter/ALL)
(def default-rate-unit TimeUnit/SECONDS)
(def default-duration-unit TimeUnit/MILLISECONDS)

(def log (LoggerFactory/getLogger StatsDReporter))

(defn info [^String message]
  (.info ^Logger log message))

(defn map->metrics [stream convert-rate convert-duration]
  (s/mapcat (fn [m]
              (serialize m convert-rate convert-duration))
            stream))

(defn -init
  ([registry host]
   (-init registry host
          comm/default-port comm/default-max-size comm/default-max-latency
          default-filter default-rate-unit default-duration-unit))
  ([registry host port max-size max-latency filter rate-unit duration-unit]
   [[registry "statsd-reporter" filter rate-unit duration-unit]
    (atom {:host        host
           :port        port
           :max-size    max-size
           :max-latency max-latency})]))

(defn -report
  ([this] (.parentReport this))
  ([this gauges counters histograms meters timers]
   (let [{:keys [metric-stream]} @(.state this)]
     @(d/zip'
        (s/put! metric-stream gauges)
        (s/put! metric-stream counters)
        (s/put! metric-stream histograms)
        (s/put! metric-stream meters)
        (s/put! metric-stream timers)))))

(defn -start [this period unit]
  (info "starting statsd metrics reporter")
  (let [{:keys [host port max-size max-latency]} @(.state this)
        client (comm/client host port max-size max-latency)
        convert-rate #(.convertRate this %)
        convert-duration #(.convertDuration this %)
        metric-stream (s/stream)]
    (s/connect
      (map->metrics metric-stream convert-rate convert-duration)
      client)

    (swap! (.state this) assoc
           :client client
           :metric-stream metric-stream)

    (.parentStart this period unit)))

(defn -stop [this]
  (let [{:keys [client]} @(.state this)]
    (try (.parentStop this)
         (finally
           (info "stopping statsd metrics reporter")
           (swap! (.state this) dissoc :client :metric-stream)
           (s/close! client)))))
