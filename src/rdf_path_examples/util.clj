(ns rdf-path-examples.util
  (:require [clojure.java.io :as io]))

(def resource->string (comp slurp io/resource)) 
