(ns rdf-path-examples.examples-test
  (:require [rdf-path-examples.examples :as examples]
            [rdf-path-examples.rdf :as rdf]
            [rdf-path-examples.util :as util]
            [rdf-path-examples.sparql :as sparql]
            [stencil.core :refer [render-file]]
            [clojure.tools.logging :as log]
            [clojure.test :refer :all]
            [clojure.java.io :as io])
  (:import [org.apache.jena.query QueryFactory QueryParseException]))

(def valid-path (rdf/json-ld->rdf-model (util/resource->input-stream "valid_path.jsonld")))

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
  (is (valid-sparql-query? (util/resource->string "sparql/extract_path.rq"))
      "SPARQL query for extracting paths is syntactically valid.")
  (let [preprocessed-path (examples/preprocess-path valid-path)]
    (is (= preprocessed-path {:path [{:start {:first true
                                              :type "<http://purl.org/goodrelations/v1#BusinessEntity>"
                                              :varname "e0"}
                                      :edgeProperty "<http://xmlns.com/foaf/0.1/page>"
                                      :end {:datatype true
                                            :type "<http://www.w3.org/2001/XMLSchema#string>"
                                            :varname "e1"}}]
                              :vars [{:varname "e0"}
                                     {:datatype true
                                      :varname "e1"}]})
        "Preprocessing works as expected.")))

(deftest extract-path-nodes
  (is (valid-sparql-query? (util/resource->string "sparql/extract_path_nodes.rq"))
      "SPARQL query for extracting path nodes is syntactically valid."))

(deftest extract-datatype-property-ranges
  (is (valid-sparql-query? (util/resource->string "sparql/datatype_property_ranges.rq"))
      "SPARQL query for extracting datatype property ranges is syntactically valid.")
  (let [get-range (comp examples/extract-datatype-property-ranges
                        rdf/turtle->rdf-model
                        util/resource->input-stream)]
    (are [file-name property-range]
         (= (get-range file-name) {"<http://example.com/property>" property-range})
         "duration_ranges.ttl" 38880000
         "date_ranges.ttl" 938908800
         "decimal_ranges.ttl" 949.56)))

(deftest extract-examples
  (let [random-examples (rdf/json-ld->rdf-model (util/resource->input-stream "random_examples.jsonld"))
        examples-map (examples/extract-examples random-examples)]))

(deftest random-selection
  (testing "Random selection generates a syntatically valid SPARQL query."
    (is (valid-template? "sparql/templates/random.mustache"))))

(deftest distinct-selection
  (testing "Distinct selection generates a syntactically valid SPARQL query."
    (is (valid-template? "sparql/templates/distinct.mustache"))))
