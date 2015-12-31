(ns rdf-path-examples.similarity-test
  (:require [rdf-path-examples.similarity :refer [compute-similarity]]
            [cljs.test :refer-macros [are deftest is testing]]))

(deftest compute-similarity-test
  (testing "Equivalent resources have similarity of 1"
    (are [resource] (= (compute-similarity (fn [_]) resource resource) 1)
         {"@value" 1}
         {"@value" "2015-12-30"}
         {"@value" "https://example.com:3030/path/to/resource"})))
