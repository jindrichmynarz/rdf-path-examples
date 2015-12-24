(ns rdf-path-examples.schema
  (:require [rdf-path-examples.prefixes :refer [rdfs]]
            [schema.core :as s :include-macros true])
  (:import [goog Uri]))

(defn- http-iri?
  "Test if `iri` is a HTTP IRI."
  [iri]
  (contains? #{"http" "https"} (.getScheme (Uri. iri))))

(def HTTP-IRI
  (s/pred http-iri? 'HTTP-IRI))

(def positive? (s/pred pos? 'pos?))

(def Config
  "Schema for configuration argument"
  {:sparql-endpoint HTTP-IRI
   :graph-iri HTTP-IRI
   :selection-method (s/enum "random")
   (s/optional-key :limit) (s/conditional positive? s/Int)})

(def Node
  "A node in path edge"
  (s/conditional map? {:type s/Str ; A node must be explicitly typed.
                       s/Keyword s/Any} ; Optional node predicates (include :type)
                 string? s/Str))

(def Edge
  "An edge in RDF path"
  {(s/optional-key :type) (s/enum "Edge")
   :start Node
   :edgeProperty s/Str
   :end Node})

(def Path
  "RDF path"
  {:type (s/enum "Path") ; Path must be explicitly typed.
   :edges [Edge]
   (s/optional-key :id) s/Str})

(def PathGraph
  "Graph of RDF paths"
  [(s/one Path "path") {s/Keyword s/Any}])
