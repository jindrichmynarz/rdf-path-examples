(ns rdf-path-examples.util
  (:require [clojure.java.io :as io]))

(def duration-regex
  "Regular expression matching xsd:duration."
  #"^-?P(\d+Y)?(\d+M)?(\d+D)?T?(\d+H)?(\d+M)?(\d+(\.\d+)?S)?$")

(defn parse-number
  "Reads a number from a string. Returns nil if not a number.
  Taken from <http://stackoverflow.com/a/12285023/385505>."
  [s]
  (when (and s (re-find #"^-?\d+\.?\d*$" s))
    (read-string s)))

(def resource->input-stream (comp io/input-stream io/resource))

(def resource->string (comp slurp io/resource))
