(ns metrics-statsd.serialization
  (:import [com.codahale.metrics Counter Gauge Histogram Metered Meter Timer]
           java.util.Collections$UnmodifiableSortedMap))

(defn prefix [name sub]
  (str name "." sub))

(defprotocol Serialize
  (serialize
    [metric convert-rate convert-duration]
    [metric name convert-rate convert-duration]))

(defn serialize-metered [^Metered meter name convert-rate]
  [{:name  (prefix name "count")
    :type  :gauge
    :value (.getCount meter)}
   {:name  (prefix name "m1_rate")
    :type  :timer
    :value (convert-rate (.getOneMinuteRate meter))}
   {:name  (prefix name "m5_rate")
    :type  :timer
    :value (convert-rate (.getFiveMinuteRate meter))}
   {:name  (prefix name "m15_rate")
    :type  :timer
    :value (convert-rate (.getFifteenMinuteRate meter))}
   {:name  (prefix name "mean_rate")
    :type  :timer
    :value (convert-rate (.getMeanRate meter))}])

(extend-protocol Serialize
  Counter
  (serialize [counter name _ _]
    [{:name  name
      :type  :counter
      :value (.getCount counter)}])

  Gauge
  (serialize [gauge name _ _]
    [{:name  name
      :type  :gauge
      :value (.getValue gauge)}])

  Histogram
  (serialize [histogram name _ _]
    (let [snapshot (.getSnapshot histogram)]
      [{:name  (prefix name "count")
        :type  :gauge
        :value (.getCount histogram)}
       {:name  (prefix name "max")
        :type  :timer
        :value (.getMax snapshot)}
       {:name  (prefix name "mean")
        :type  :timer
        :value (.getMean snapshot)}
       {:name  (prefix name "min")
        :type  :timer
        :value (.getMin snapshot)}
       {:name  (prefix name "stddev")
        :type  :timer
        :value (.getStdDev snapshot)}
       {:name  (prefix name "p50")
        :type  :timer
        :value (.getMedian snapshot)}
       {:name  (prefix name "p75")
        :type  :timer
        :value (.get75thPercentile snapshot)}
       {:name  (prefix name "p95")
        :type  :timer
        :value (.get95thPercentile snapshot)}
       {:name  (prefix name "p98")
        :type  :timer
        :value (.get98thPercentile snapshot)}
       {:name  (prefix name "p99")
        :type  :timer
        :value (.get99thPercentile snapshot)}
       {:name  (prefix name "p999")
        :type  :timer
        :value (.get999thPercentile snapshot)}]))

  Meter
  (serialize [meter name convert-rate _]
    (serialize-metered meter name convert-rate))

  Timer
  (serialize [timer name convert-rate convert-duration]
    (let [snapshot (.getSnapshot timer)]
      (concat
        [{:name  (prefix name "max")
          :type  :timer
          :value (convert-duration (.getMax snapshot))}
         {:name  (prefix name "mean")
          :type  :timer
          :value (convert-duration (.getMean snapshot))}
         {:name  (prefix name "min")
          :type  :timer
          :value (convert-duration (.getMin snapshot))}
         {:name  (prefix name "stddev")
          :type  :timer
          :value (convert-duration (.getStdDev snapshot))}
         {:name  (prefix name "p50")
          :type  :timer
          :value (convert-duration (.getMedian snapshot))}
         {:name  (prefix name "p75")
          :type  :timer
          :value (convert-duration (.get75thPercentile snapshot))}
         {:name  (prefix name "p95")
          :type  :timer
          :value (convert-duration (.get95thPercentile snapshot))}
         {:name  (prefix name "p98")
          :type  :timer
          :value (convert-duration (.get98thPercentile snapshot))}
         {:name  (prefix name "p99")
          :type  :timer
          :value (convert-duration (.get99thPercentile snapshot))}
         {:name  (prefix name "p999")
          :type  :timer
          :value (convert-duration (.get999thPercentile snapshot))}]
        (serialize-metered timer name convert-rate))))

  Collections$UnmodifiableSortedMap
  (serialize [m convert-rate convert-duration]
    (mapcat (fn [[name metric]]
              (serialize metric name convert-rate convert-duration))
            (into {} m))))
