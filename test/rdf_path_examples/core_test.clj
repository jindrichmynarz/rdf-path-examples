(ns rdf-path-examples.core-test
  (:require [rdf-path-examples.core :refer [app]]
            [rdf-path-examples.examples :as examples]
            [rdf-path-examples.util :refer [resource->string]]
            [rdf-path-examples.sparql :refer [ask-query]]
            [rdf-path-examples.json-ld :refer [json-ld->rdf-dataset]]
            [clojure.test :refer :all]
            [ring.mock.request :as mock])
  (:import [java.io ByteArrayInputStream]))

(def jsonld "application/ld+json")

(def status-code (comp :status app))

(def valid-path (resource->string "valid_path.jsonld"))

(deftest ^:integration routes
  (testing "Invalid URLs are not found."
    (is (= (status-code (mock/request :get "/invalid")) 404))))

(deftest ^:integration generate-examples-test
  (let [valid-params {:selection-method (rand-nth (-> examples/generate-examples methods keys)) 
                      :sparql-endpoint "http://localhost:8890/sparql"}
        generate-examples (fn [& {:keys [accept body content-type method params]
                                  :or {accept jsonld
                                       body valid-path
                                       content-type jsonld
                                       method :post
                                       params {}}}]
                            (-> (mock/request method "/generate-examples")
                                (mock/content-type content-type)
                                (mock/header "Accept" accept)
                                (mock/query-string (merge valid-params params))
                                (mock/body body)))
        has-constraint-violation-query (resource->string "has_constraint_violation.rq")
        is-400? (comp (partial = 400) status-code)
        has-constraint-violation? (comp #(ask-query % has-constraint-violation-query)
                                        json-ld->rdf-dataset
                                        #(ByteArrayInputStream. (.getBytes %))
                                        :body
                                        app
                                        generate-examples)]
    (testing "Requests are sent using HTTP POST"
      (is (= (status-code (generate-examples :method :get)) 405)))
    (testing "Other input MIME types than JSON-LD are not supported"
      (is (= (status-code (generate-examples :content-type "application/x-bork-bork-bork")) 415)))
    (testing "Other output MIME types than JSON-LD are not supported"
      (is (= (status-code (generate-examples :accept "application/x-bork-bork-bork")) 406)))
    (testing "Query string parameters are well-formed"
      (is (is-400? (generate-examples :params {:limit (- (rand-int Integer/MAX_VALUE))})) 
          "limit must be a positive integer.")
      (is (is-400? (generate-examples :params {:sparql-endpoint "ftp://invalid"}))
          "sparql-endpoint must be a valid HTTP IRI.")
      (is (is-400? (generate-examples :params {:selection-method "invalid"}))
          "selection-method must be supported."))
    (testing "RDF path must not be empty"
      (let [body (resource->string "no_path.jsonld")]
        (is (is-400? (generate-examples :body body)))
        (is (has-constraint-violation? :body body))))
    (is (is-400? (generate-examples :body (resource->string "missing_edge_property.jsonld")))
        "Edges of RDF path must have rpath:start, rpath:edgeProperty, and rpath:end.")
    (is (is-400? (generate-examples :body (resource->string "discontinuous_path.jsonld")))
        "Paths must be continuous.")
    (is (is-400? (generate-examples :body (resource->string "start_datatype.jsonld")))
        "Only edge ends can be datatypes.")
    (is (is-400? (generate-examples :body (resource->string "missing_type.jsonld")))
        "Edge starts and ends must have a type.")
    (is (is-400? (generate-examples :body (resource->string "more_than_1_path.jsonld")))
        "Only 1 RDF path may be provided.")))
