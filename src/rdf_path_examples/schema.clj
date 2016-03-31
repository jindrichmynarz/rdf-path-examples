(ns rdf-path-examples.schema
  (:require [schema.core :as s]
            [schema-contrib.core :as sc]))

(def http? (partial re-matches #"^https?:\/\/.*$"))

(def positive-integer
  (s/constrained s/Int pos? 'pos?))

(def Config
  "Schema for configuration parameters"
  {(s/required-key "sparql-endpoint") (s/constrained sc/URI http? 'http?) 
   (s/required-key "selection-method") (s/enum "random" "distinct" "representative")
   (s/optional-key "graph-iri") sc/URI
   (s/optional-key "limit") positive-integer
   (s/optional-key "sampling-factor") positive-integer})
