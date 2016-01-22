(ns rdf-path-examples.type-inference-test
  (:require [rdf-path-examples.type-inference :refer [infer-datatype infer-type]]
            [rdf-path-examples.xml-schema :as xsd]
            [rdf-path-examples.prefixes :as prefix]
            [cljs.test :refer-macros [are deftest is testing]]))

(deftest infer-type-test
  (testing "Matching resources"
    (are [resource resource-type] (= (infer-type resource) resource-type)
         {"@id" "_:b1234"} :referent
         {"@value" "https://example.com:3030/path/to/resource"} ::xsd/anyURI
         {"@value" "2000-01-01"
          "@datatype" (prefix/xsd "date")} ::xsd/date
         {"@value" "2015-12-30"} ::xsd/date
         {"@value" 1} ::xsd/decimal
         {"@value" 1.2} ::xsd/decimal
         {"@value" false} ::xsd/boolean)))

(deftest infer-datatype-test
  (testing "Matching literals"
    (are [literal literal-type] (= (infer-datatype literal) literal-type)
         "2000-01-01" ::xsd/date
         "2015-12-28T12:03:00+01:00" ::xsd/dateTime
         "P7Y7D12.34S" ::xsd/duration
         "http://example.com:8080/path/to/page" ::xsd/anyURI
         "example.com" :literal))
  (testing "Non-matching literals"
    (are [literal literal-type] (not= (infer-datatype literal) literal-type)
         "example.com" ::xsd/anyURI)))
