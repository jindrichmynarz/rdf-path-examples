(ns rdf-path-examples.type-inference-test
  (:require [rdf-path-examples.type-inference :as infer]
            [rdf-path-examples.xml-schema :as xsd]
            [rdf-path-examples.prefixes :as prefix]
            [clojure.test :refer :all]))

(deftest infer-type-test
  (testing "Matching resources"
    (are [resource resource-type] (= (infer/infer-type resource) resource-type)
         {"@id" "_:b1234"} :referent
         {"@value" "https://example.com:3030/path/to/resource"} ::xsd/anyURI
         {"@value" "2000-01-01"
          "@type" (prefix/xsd "date")} ::xsd/date
         {"@type" (prefix/xsd "decimal") 
          "@value" "113496506.71"} ::xsd/decimal
         {"@value" "2015-12-30"} ::xsd/date
         {"@value" 1} ::xsd/decimal
         {"@value" 1.2} ::xsd/decimal
         {"@value" false} ::xsd/boolean)))

(deftest infer-datatype-test
  (testing "Matching literals"
    (are [literal literal-type] (= (infer/infer-datatype literal) literal-type)
         "2000-01-01" ::xsd/date
         "2015-12-28T12:03:00+01:00" ::xsd/dateTime
         "P7Y7D12.34S" ::xsd/duration
         "http://example.com:8080/path/to/page" ::xsd/anyURI
         "example.com" ::xsd/string))
  (testing "Non-matching literals"
    (are [literal literal-type] (not= (infer/infer-datatype literal) literal-type)
         "example.com" ::xsd/anyURI)))

(deftest lowest-common-ancestor-test
  (derive ::a ::b)
  (derive ::b ::c)
  (derive ::d ::c)
  (derive ::c ::e)
  (are [a b ancestor] (= (infer/lowest-common-ancestor a b) ancestor)
       ::a ::d ::c
       ::c ::e ::e
       ::b ::d ::c
       ::a ::non-existent nil
       :referent :referent :referent))

(deftest is-ordinal-test
  (testing "Ordinal data types"
    (are [data-type] (infer/is-ordinal? data-type)
         ::xsd/decimal
         ::xsd/short))
  (testing "Not ordinal data types"
    (are [data-type] (not (infer/is-ordinal? data-type))
         ::xsd/string
         ::xsd/ID)))
