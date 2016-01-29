(ns rdf-path-examples.similarity-test
  (:require [rdf-path-examples.similarity :as sim]
            [cljs.test :refer-macros [are deftest is testing]]
            [cljs-time.core :as t]))

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

(deftest duration->period-test
  (testing "Parsing durations"
    (are [input output] (t/= (sim/duration->period input) (t/map->Period output))
         "P1Y" {:years 1}
         "P5Y12M31D23H59M59.99S" {:years 5 :months 12 :days 31 :hours 23 :minutes 59 :seconds 59.99})))

(deftest duration->date-time-test
  (testing "Zero duration does not change the fixed date"
    (are [empty-duration] (t/= sim/fixed-date (sim/duration->date-time empty-duration))
         "P0Y"
         "-P0M")))
