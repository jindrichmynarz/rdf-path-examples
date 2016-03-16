(ns rdf-path-examples.sparql-test
  (:require [rdf-path-examples.sparql :as sparql]
            [clojure.test :refer :all])
  (:import [org.apache.jena.rdf.model ModelFactory]))

(deftest node->clj
  (testing "Conversion of RDF literals to Clojure values."
    (let [model (ModelFactory/createDefaultModel)
          bool (rand-nth [true false])]
      (is (= (sparql/node->clj (.createTypedLiteral model bool)) bool) 
          "Booleans are correctly casted."))))
