(ns rdf-path-examples.sparql
  (:require [rdf-path-examples.xml-schema :as xsd]
            [rdf-path-examples.type-inference :as infer]
            [clojure.tools.logging :as log])
  (:import [org.apache.jena.rdf.model Literal Model Resource]
           [org.apache.jena.query QueryExecutionFactory QueryFactory]
           [org.apache.jena.query Dataset]
           [org.apache.jena.update UpdateAction UpdateFactory]
           [org.apache.jena.datatypes.xsd XSDDatatype]))

; ----- Multimethods -----

(defmulti literal->clj
  "Convert RDF literal to a Clojure scalar data type."
  (fn [{datatype "@type"}]
    (when (infer/xml-schema-data-type? datatype)
      (infer/data-type->xml-schema datatype))))

(defmethod literal->clj ::xsd/duration
  [literal]
  (update literal "@value" str))

(defmethod literal->clj ::xsd/integer
  ; Prevent casting integers to doubles.
  [literal]
  literal)

(defmethod literal->clj ::xsd/decimal
  [literal]
  (update literal "@value" double))

(defmethod literal->clj ::xsd/date
  [literal]
  (update literal "@value" str))

(defmethod literal->clj ::xsd/dateTime
  [literal]
  (update literal "@value" str))

(defmethod literal->clj :default
  [{value "@value"
    :as literal}]
  literal)

; ----- Protocols -----

(defprotocol IStringifiableNode
  "Returns string representation of RDF node"
  (node->clj [node]))

(extend-protocol IStringifiableNode
  Literal
  (node->clj [node]
    (let [datatype (.getDatatypeURI node)]
      (literal->clj (cond-> {"@value" (.getValue node)}
                      datatype (assoc "@type" datatype)))))
  
  Resource
  (node->clj [node]
    (if (.isAnon node)
      {"@id" (str "_:" (.getId node))}
      {"@id" (.getURI node)})))

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
  "Execute SPARQL ASK `query` on RDF `model`."
  [^Model model
   ^String query]
  (with-open [qexec (QueryExecutionFactory/create query model)]
    (.execAsk qexec)))

(defmulti construct-query
  "Execute SPARQL CONSTRUCT `query` on `datasource`."
  (fn [datasource query] (type datasource)))

(defmethod ^Model construct-query Model
  ; Execute SPARQL CONSTRUCT `query` on local RDF `model`.
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

(defn ^Model describe-query
  "Execute SPARQL DESCRIBE `query` on local RDF `model`."
  [^Model model
   ^String query]
  (with-open [qexec (QueryExecutionFactory/create query model)]
    (.execDescribe qexec)))

(defmulti select-query
  "Execute SPARQL SELECT `query` on `datasource`." 
  (fn [datasource query] (type datasource)))

(defmethod select-query Model
  ; Execute SPARQL SELECT `query` on local RDF `model`.
  [^Model model
   ^String query]
  (with-open [qexec (QueryExecutionFactory/create query model)]
    (let [results (.execSelect qexec)
          result-vars (.getResultVars results)]
      (mapv (partial process-select-solution result-vars)
            (iterator-seq results)))))

(defn ^Model update-operation
  "Execute SPARQL Update `operation` on `model`."
  [^Model model
   ^String operation]
  (UpdateAction/execute (UpdateFactory/create operation) model)
  model)
