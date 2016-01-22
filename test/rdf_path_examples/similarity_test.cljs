(ns rdf-path-examples.similarity-test
  (:require [rdf-path-examples.similarity :as sim]
            [cljs.test :refer-macros [are deftest is testing]]))

(deftest compute-similarity-test
  (let [similarity-fn (partial sim/compute-similarity (fn [_]))] ; Mocked similarity function
    (testing "Equivalent resources have similarity of 1"
      (are [resource] (= (similarity-fn resource resource) 1)
           {"@value" 1}
           {"@value" "2015-12-30"}
           {"@value" "https://example.com:3030/path/to/resource"}))
    (testing "Similarity is symmetric"
      (are [a b] (= (similarity-fn a b) (similarity-fn b a))
           {"@value" 1} {"@value" 3}
           {"@value" "http://example.com/path/to/a/file"} {"@value" "https://example.com/path/to/another/file"}
           {"@id" "_:b1"} {"@id" "_:b2"}))))
