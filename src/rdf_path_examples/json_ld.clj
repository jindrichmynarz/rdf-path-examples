(ns rdf-path-examples.json-ld
  (:require [clojure.java.io :as io])
  (:import [java.io InputStream]
           [java.util LinkedHashMap]
           [com.github.jsonldjava.core JsonLdOptions JsonLdProcessor]
           [com.github.jsonldjava.utils JsonUtils]
           [org.apache.jena.rdf.model Model ModelFactory]))

(defonce ^:private
  json-ld-options
  (doto (JsonLdOptions.) (.setUseNativeTypes true)))

(defn ^Model json-ld->rdf-model
  "Convert `json-ld` to Jena RDF model."
  [^InputStream json-ld]
  (.read (ModelFactory/createDefaultModel) json-ld nil "JSON-LD"))

(defn compact
  "Compact `json-ld` using `context` with optional `options`"
  [^LinkedHashMap json-ld
   ^LinkedHashMap context
   & {:keys [options]
      :or {options json-ld-options}}]
  (JsonLdProcessor/compact json-ld context options))

(def load-resource
  "Load JSON resource from `resource-path`."
  (comp #(JsonUtils/fromInputStream %) io/input-stream io/resource))

(def load-context
  "Load JSON-LD context with `file-name`."
  (comp load-resource (partial str "contexts/")))
