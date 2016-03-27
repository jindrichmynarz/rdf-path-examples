(ns rdf-path-examples.examples-test
  (:require [rdf-path-examples.examples :as examples]
            [rdf-path-examples.json-ld :refer [json-ld->rdf-dataset]]
            [stencil.core :refer [render-file]]
            [clojure.test :refer :all]
            [clojure.java.io :as io])
  (:import [org.apache.jena.query QueryFactory QueryParseException]))

(def valid-path (json-ld->rdf-dataset (io/input-stream (io/resource "valid_path.jsonld"))))

(def configuration
  {:limit 5
   :selection-method "random"
   :sparql-endpoint "http://lod2-dev.vse.cz:8890/sparql"})

(deftest preprocess-path
  (let [preprocessed-path (examples/preprocess-path valid-path)]
    (is (= preprocessed-path {:path [{:start {:first true
                                              :type "http://purl.org/goodrelations/v1#BusinessEntity"
                                              :varname "e0"}
                                      :edgeProperty "http://xmlns.com/foaf/0.1/page"
                                      :end {:datatype true
                                            :type "http://www.w3.org/2001/XMLSchema#string"
                                            :varname "e1"}}]
                              :vars [{:varname "e0"}
                                     {:datatype true
                                      :varname "e1"}]})
        "Preprocessing works as expected.")))

(deftest random-selection
  (testing "Random selection generates syntatically valid SPARQL query."
    (is (QueryFactory/create (render-file "sparql/query_templates/random.mustache"
                                          (assoc (examples/preprocess-path valid-path)
                                                 :limit (:limit configuration)))))))
