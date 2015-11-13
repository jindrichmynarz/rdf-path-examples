(ns rdf-path-examples.schema
  (:require [schema.core :as s :include-macros true])
  (:import [goog Uri]))

(defn http-iri?
  "Test if `iri` is a HTTP IRI."
  [iri]
  (contains? #{"http" "https"} (.getScheme (Uri. iri))))

(def HTTP-IRI
  (s/pred http-iri? 'HTTP-IRI))

(def Config
  "Schema for configuration argument"
  {:sparql-endpoint HTTP-IRI
   :graph-iri HTTP-IRI
   :selection-method (s/enum "random")})
