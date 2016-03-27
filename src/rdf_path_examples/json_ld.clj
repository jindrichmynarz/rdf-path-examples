(ns rdf-path-examples.json-ld
  (:require [clojure.java.io :as io])
  (:import [java.io InputStream]
           [java.util LinkedHashMap]
           [com.github.jsonldjava.core JsonLdOptions JsonLdProcessor]
           [com.github.jsonldjava.utils JsonUtils]
           [org.apache.jena.query Dataset DatasetFactory]
           [org.apache.jena.riot Lang RDFDataMgr]))

(defonce ^:private
  json-ld-options
  (doto (JsonLdOptions.) (.setUseNativeTypes true)))

(defn ^Dataset json-ld->rdf-dataset
  "Convert input stream `json-ld` into Jena Dataset."
  [^InputStream json-ld]
  (let [dataset (DatasetFactory/create)]
    (RDFDataMgr/read dataset json-ld Lang/JSONLD)
    dataset))

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
