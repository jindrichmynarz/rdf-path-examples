(ns rdf-path-examples.json-ld
  (:require [clojure.java.io :as io])
  (:import [java.util LinkedHashMap]
           [com.github.jsonldjava.core JsonLdOptions JsonLdProcessor]
           [com.github.jsonldjava.utils JsonUtils]))

(defonce ^:private
  json-ld-options
  (doto (JsonLdOptions.) (.setUseNativeTypes true)))

(defn compact
  "Compact `json-ld` using `context` with optional `options`"
  [^LinkedHashMap json-ld
   ^LinkedHashMap context
   & {:keys [options]
      :or {options json-ld-options}}]
  (JsonLdProcessor/compact json-ld context options))

(defn expand
  "Expand `json-ld` with optional `options`."
  [^LinkedHashMap json-ld
   & {:keys [options]
      :or {options json-ld-options}}]
  (JsonLdProcessor/expand json-ld options))

(def load-resource
  "Load JSON resource from `resource-path`."
  (comp #(JsonUtils/fromInputStream %) io/input-stream io/resource))

(def load-context
  "Load JSON-LD context with `file-name`."
  (comp load-resource (partial str "contexts/")))
