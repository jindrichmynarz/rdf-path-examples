(ns rdf-path-examples.sparql-test
  (:require [rdf-path-examples.sparql :as sparql]
            [rdf-path-examples.util :refer [resource->string]]
            [clojure.test :refer :all]
            [clojure.tools.logging :as log])
  (:import [org.apache.jena.rdf.model ModelFactory]
           [org.apache.jena.datatypes.xsd XSDDatatype]
           [org.apache.jena.datatypes BaseDatatype]))

(deftest node->clj
  (let [model (ModelFactory/createDefaultModel)]
    (testing "Conversion of plain RDF literals to Clojure values."
      (letfn [(convert [literal]
                (get (sparql/node->clj (.createTypedLiteral model literal)) "@value"))]
        (are [literal] (= (convert literal) literal)
          false
          9
          3.14)))
    (testing "Conversion of datatyped RDF literals to Clojure values."
      (letfn [(convert [literal datatype]
                (get (sparql/node->clj (.createTypedLiteral model literal datatype)) "@value"))]
        (are [literal datatype] (= (convert literal datatype) literal)
          "2014-02-27" XSDDatatype/XSDdate
          "2014-02-27T12:00:00Z" XSDDatatype/XSDdateTime
          "40" (BaseDatatype. "http://purl.org/procurement/public-contracts-datatypes#percentage"))))))

(deftest update-operation
  (testing "Execution of SPARQL Update operations"
    (let [model (ModelFactory/createDefaultModel)
          operation (resource->string "test_update.ru")
          ask (resource->string "test_update.rq")]
      (is (sparql/ask-query (sparql/update-operation model operation) ask)))))
