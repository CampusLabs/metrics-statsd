(ns metrics-statsd.core
  (:require [metrics-statsd
             [communication :as comm]
             [reporter :as reporter]])
  (:import metrics-statsd.reporter.StatsDReporter))

(defn new-statsd-reporter
  ([registry] (new-statsd-reporter registry {}))
  ([registry {:keys [host port max-size max-latency
                     filter rate-unit duration-unit]
              :or   {host          reporter/default-host
                     port          comm/default-port
                     max-size      comm/default-max-size
                     max-latency   comm/default-max-latency
                     filter        reporter/default-filter
                     rate-unit     reporter/default-rate-unit
                     duration-unit reporter/default-duration-unit}}]
   (StatsDReporter. registry host port max-size max-latency
                    filter rate-unit duration-unit)))
