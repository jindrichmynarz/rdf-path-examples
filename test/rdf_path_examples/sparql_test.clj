(ns rdf-path-examples.sparql-test
  (:require [rdf-path-examples.sparql :as sparql]
            [rdf-path-examples.util :refer [resource->string]]
            [clojure.test :refer :all])
  (:import [org.apache.jena.rdf.model ModelFactory]))

(deftest node->clj
  (testing "Conversion of RDF literals to Clojure values."
    (let [model (ModelFactory/createDefaultModel)
          bool (rand-nth [true false])]
      (is (= (get (sparql/node->clj (.createTypedLiteral model bool)) "@value") bool) 
          "Booleans are correctly casted."))))

(deftest update-operation
  (testing "Execution of SPARQL Update operations"
    (let [model (ModelFactory/createDefaultModel)
          operation (resource->string "test_update.ru")
          ask (resource->string "test_update.rq")]
      (is (sparql/ask-query (sparql/update-operation model operation) ask)))))
