(ns rdf-path-examples.examples-test
  (:require [rdf-path-examples.examples :as examples]
            [rdf-path-examples.json-ld :refer [json-ld->rdf-model]]
            [stencil.core :refer [render-file]]
            [clojure.tools.logging :as log]
            [clojure.test :refer :all]
            [clojure.java.io :as io])
  (:import [org.apache.jena.query QueryFactory QueryParseException]))

(def valid-path (json-ld->rdf-model (io/input-stream (io/resource "valid_path.jsonld"))))

(def configuration
  {:limit 5
   :sampling-factor 20
   :selection-method "random"
   :sparql-endpoint "http://lod2-dev.vse.cz:8890/sparql"})

(defn- valid-sparql-query?
  "Test if SPARQL `query` is syntactically valid."
  [^String query]
  (try (QueryFactory/create query)
       true
       (catch QueryParseException e
         (log/error (.getMessage e))
         false)))

(defn- valid-template?
  "Test if rendering the template from `template-path` using the default valid configuration
  results in a syntactically valid SPARQL query."
  [^String template-path]
  (let [query (render-file template-path
                           (assoc (examples/preprocess-path valid-path)
                                  :limit (:limit configuration)))]
    (valid-sparql-query? query)))

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
  (testing "Random selection generates a syntatically valid SPARQL query."
    (is (valid-template? "sparql/templates/random.mustache"))))

(deftest distinct-selection
  (testing "Distinct selection generates a syntactically valid SPARQL query."
    (is (valid-template? "sparql/templates/distinct.mustache"))))
