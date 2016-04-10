(ns rdf-path-examples.distance-test
  (:require [rdf-path-examples.distance :as distance]
            [clojure.test :refer :all]))

(deftest parse-duration
  (are [s period] (= (distance/parse-duration s) period)
       "P1M" (distance/->period :months 1)
       "P2000Y5M30D" (distance/->period :years 2000 :months 5 :days 30)
       "-P120D" (distance/->period :days -120)
       "P1Y2MT2H" (distance/->period :years 1 :months 2 :hours 2)
       "PT0.5S" (distance/->period :seconds 0.5)))

(deftest compute-distance
  (let [distance-fn (partial distance/compute-distance (fn []) {})] ; Mocked distance function
    (testing "Equivalent resources have distance of 0"
      (are [resource] (= (distance-fn resource resource) 0)
           {"@value" 1}
           {"@value" "2015-12-30"}
           {"@value" "https://example.com:3030/path/to/resource"}))
    (testing "Distance is symmetric"
      (are [a b] (= (distance-fn a b) (distance-fn b a))
           {"@value" 1} {"@value" 3}
           {"@value" "http://example.com/path/to/a/file"} {"@value" "https://example.com/path/to/another/file"}
           {"@id" "_:b1"} {"@id" "_:b2"}))))
