(ns metrics-statsd.codec
  (:require [clojure.set :refer [map-invert]]
            [clojure.string :as s]
            [gloss.core :as g]))

(def invalid-characters #"[:\|@]")

(def metric-name
  (g/compile-frame
    (g/string :utf-8 :delimiters [":"])
    #(s/replace % invalid-characters "_")
    identity))

(def metric-value
  (g/delimited-frame "|"
    (g/string-float :utf-8)))

(def metric-type->key
  {:gauge     "g"
   :counter   "c"
   :timer     "ms"
   :histogram "h"
   :meter     "m"
   :set       "s"})

(def key->metric-type
  (map-invert metric-type->key))

(def metric-type
  (g/compile-frame
    (g/string :utf-8)
    metric-type->key
    key->metric-type))

(def metric-codec
  (g/delimited-frame "\n"
    (g/ordered-map
      :name metric-name
      :value metric-value
      :type metric-type)))
