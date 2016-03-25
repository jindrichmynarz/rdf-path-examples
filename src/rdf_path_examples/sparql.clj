(ns rdf-path-examples.sparql
  (:import [org.apache.jena.rdf.model Literal Model Resource]
           [org.apache.jena.query QueryExecutionFactory QueryFactory]))

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

(defmulti construct-query
  "Execute SPARQL CONSTRUCT `query` on `datasource`."
  (fn [datasource query] (type datasource)))

(defmethod ^Model construct-query Model
  ; Execute SPARQL CONSTRUCT `query` on local RDF model `model`.
  [^Model model
   ^String query]
  (with-open [qexec (QueryExecutionFactory/create query model)]
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

(defmethod select-query Model
  ; Execute SPARQL SELECT `query` on local RDF model `model`.
  [^Model model
   ^String query]
  (with-open [qexec (QueryExecutionFactory/create query model)]
    (let [results (.execSelect qexec)
          result-vars (.getResultVars results)]
      (mapv (partial process-select-solution result-vars)
            (iterator-seq results)))))