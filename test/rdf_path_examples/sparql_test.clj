(ns rdf-path-examples.sparql-test
  (:require [rdf-path-examples.sparql :as sparql]
            [rdf-path-examples.util :refer [resource->string]]
            [clojure.test :refer :all]
            [clojure.tools.logging :as log])
  (:import [org.apache.jena.rdf.model ModelFactory]))

(deftest node->clj
  (testing "Conversion of RDF literals to Clojure values."
    (let [model (ModelFactory/createDefaultModel)
          convert (fn [literal] (get (sparql/node->clj (.createTypedLiteral model literal)) "@value"))]
      (are [literal] (= (convert literal) literal)
           false
           9
           3.14))))

(deftest update-operation
  (testing "Execution of SPARQL Update operations"
    (let [model (ModelFactory/createDefaultModel)
          operation (resource->string "test_update.ru")
          ask (resource->string "test_update.rq")]
      (is (sparql/ask-query (sparql/update-operation model operation) ask)))))
