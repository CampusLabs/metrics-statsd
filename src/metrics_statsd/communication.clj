(ns metrics-statsd.communication
  (:require [aleph.udp :as udp]
            [gloss.core :as g]
            [gloss.io :as io]
            [manifold
             [deferred :as d]
             [stream :as s]]
            [metrics-statsd.codec :refer [metric-codec]]))

(def default-port (int 8125))
(def default-max-size (int 1432))
(def default-max-latency (int 20))

(defn encoder-stream [max-size max-latency host port]
  (let [sink (s/stream* {:xform (map #(io/encode metric-codec %))})
        source (->> sink
                    (s/batch g/byte-count max-size max-latency)
                    (s/map (fn [m]
                             {:host    host
                              :port    port
                              :message (seq m)})))]
    (s/splice sink source)))

(defn client
  ([host] (client host default-port))
  ([host port] (client host port default-max-size default-max-latency))
  ([host port max-length max-latency]
   (let [enc (encoder-stream max-length max-latency host port)
         socket (udp/socket {})]
     (d/chain' socket
       (fn [socket]
         (s/on-closed enc #(s/close! socket))
         (s/connect enc socket)))
     enc)))

(defn server
  ([] (server default-port))
  ([port]
   (let [socket (udp/socket {:port port})
         output-stream (s/stream)]
     (d/chain' socket
       (fn [socket]
         (s/connect (->> (s/map :message socket)
                         (s/mapcat #(io/decode-all metric-codec %)))
                    output-stream)
         (s/on-closed output-stream #(s/close! socket))))
     output-stream)))
