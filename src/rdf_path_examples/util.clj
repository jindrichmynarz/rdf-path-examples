(ns rdf-path-examples.util
  (:require [clojure.java.io :as io]))

(def duration-regex
  "Regular expression matching xsd:duration."
  #"^-?P(\d+Y)?(\d+M)?(\d+D)?T?(\d+H)?(\d+M)?(\d+(\.\d+)?S)?$")

(def resource->string (comp slurp io/resource)) 
