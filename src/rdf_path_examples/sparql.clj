(ns rdf-path-examples.sparql
  (:require [clojure.tools.logging :as log])
  (:import [org.apache.jena.rdf.model Literal Resource]
           [org.apache.jena.query QueryExecutionFactory QueryFactory]
           [org.apache.jena.query Dataset]))

; ----- Protocols -----

(defprotocol IStringifiableNode
  "Returns string representation of RDF node"
  (node->clj [node]))

(extend-protocol IStringifiableNode
  Literal
  (node->clj [node] (.getValue node)))

(extend-protocol IStringifiableNode
  Resource
  (node->clj [node] (str node)))

; ----- Private functions -----

(defn- process-select-binding
  [sparql-binding variable]
  [(keyword variable) (node->clj (.get sparql-binding variable))])

(defn- process-select-solution
  "Process SPARQL SELECT `solution` for `result-vars`."
  [result-vars solution]
  (into {} (mapv (partial process-select-binding solution) result-vars)))
 
; ----- Public functions -----

(defn ^Boolean ask-query
  "Execute SPARQL ASK `query` on RDF `dataset`."
  [^Dataset dataset
   ^String query]
  (with-open [qexec (QueryExecutionFactory/create query dataset)]
    (.execAsk qexec)))

(defmulti construct-query
  "Execute SPARQL CONSTRUCT `query` on `datasource`."
  (fn [datasource query] (type datasource)))

(defmethod ^Model construct-query Dataset
  ; Execute SPARQL CONSTRUCT `query` on local RDF `dataset`.
  [^Dataset dataset
   ^String query]
  (with-open [qexec (QueryExecutionFactory/create query dataset)]
    (.execConstruct qexec)))

(defmethod ^Model construct-query String
  ; Execute SPARQL CONSTRUCT `query` on remote SPARQL endpoint `endpoint`.
  [^String endpoint
   ^String query]
  (with-open [qexec (QueryExecutionFactory/sparqlService endpoint query)]
    (.execConstruct qexec)))

(defmulti select-query
  "Execute SPARQL SELECT `query` on `datasource`." 
  (fn [datasource query] (type datasource)))

(defmethod select-query Dataset
  ; Execute SPARQL SELECT `query` on local RDF `dataset`.
  [^Dataset dataset
   ^String query]
  (with-open [qexec (QueryExecutionFactory/create query dataset)]
    (let [results (.execSelect qexec)
          result-vars (.getResultVars results)]
      (mapv (partial process-select-solution result-vars)
            (iterator-seq results)))))
