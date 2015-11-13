(ns rdf-path-examples.macros
  (:require [clojure.java.io :refer [resource]]))

(defmacro read-file
  [file-path]
  (slurp (resource file-path)))
