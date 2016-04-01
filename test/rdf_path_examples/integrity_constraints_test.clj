(ns rdf-path-examples.integrity-constraints-test
  (:require [rdf-path-examples.integrity-constraints :as integrity]
            [rdf-path-examples.sparql :refer [ask-query]]
            [rdf-path-examples.util :refer [resource->input-stream resource->string]]
            [rdf-path-examples.rdf :refer [json-ld->rdf-model]]
            [clojure.test :refer :all]))

(deftest validate-path
  (let [has-constraint-violation-query (resource->string "has_constraint_violation.rq")
        validate (comp integrity/validate-path
                       json-ld->rdf-model
                       resource->input-stream)
        has-constraint-violation? (comp #(ask-query % has-constraint-violation-query)
                                        validate)]
    (is (nil? (validate "valid_path.jsonld"))
        "Valid path")
    (is (has-constraint-violation? "no_path.jsonld")
        "RDF path must not be empty")
    (is (has-constraint-violation? "missing_edge_property.jsonld")
      "Edges of RDF path must have rpath:start, rpath:edgeProperty, and rpath:end.")
    (testing "Paths must be continuous."
      (is (nil? (validate "continuous_path.jsonld")))
      (is (has-constraint-violation? "discontinuous_path.jsonld")))
    (is (has-constraint-violation? "start_datatype.jsonld")
      "Only edge ends can be datatypes.")
    (is (has-constraint-violation? "missing_type.jsonld")
        "Edge starts and ends must have a type.")
    (is (has-constraint-violation? "more_than_1_path.jsonld")
        "Only 1 RDF path may be provided.")))
