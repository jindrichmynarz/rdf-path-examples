(ns rdf-path-examples.jsonld
  (:import [java.io InputStream]
           [java.util LinkedHashMap]
           [com.github.jsonldjava.core JsonLdOptions JsonLdProcessor]
           [org.apache.jena.rdf.model Model ModelFactory]))

(defonce ^:private
  json-ld-options
  (doto (JsonLdOptions.) (.setUseNativeTypes true)))

(defn ^Model json-ld->rdf-model
  "Convert `json-ld` to Jena RDF model."
  [^InputStream json-ld]
  (.read (ModelFactory/createDefaultModel) json-ld nil "JSON-LD"))

(defn compact-json-ld
  "Compact `json-ld` using `context` with optional `options`"
  [^LinkedHashMap json-ld
   ^LinkedHashMap context
   & {:keys [options]
      :or {options json-ld-options}}]
  (JsonLdProcessor/compact json-ld context options))
