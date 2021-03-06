(ns rdf-path-examples.integrity-constraints
  (:require [rdf-path-examples.sparql :refer [construct-query]]
            [clojure.java.io :as io])
  (:import [org.apache.jena.rdf.model Model]
           [org.apache.jena.query Dataset]
           [com.github.jsonldjava.utils JsonUtils]))

(defonce integrity-constraints
  (->> "sparql/integrity_constraints"
       io/resource
       io/as-file
       file-seq
       (filter #(.isFile %))
       (sort-by #(.getName %))
       (map slurp)
       doall))

(defn- ^Model execute-validation
  "Execute validation of integrity constraints of RDF `path`."
  [^Model path]
  (->> integrity-constraints
       (map (partial construct-query path))
       (reduce (fn [m1 m2] (.union m1 m2)))))

(defn- ^Boolean valid-path?
  "Test if `validation-results` indicate a valid RDF path."
  [^Model validation-results]
  (zero? (.size validation-results)))

(defn validate-path
  "Validate RDF `path`.
  Returns nil if path is valid. Otherwise, returns RDF model containing validation errors."
  [^Model path]
  (let [validation-results (execute-validation path)]
    (when-not (valid-path? validation-results)
      validation-results)))
