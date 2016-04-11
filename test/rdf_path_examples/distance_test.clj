(ns rdf-path-examples.distance-test
  (:require [rdf-path-examples.distance :as distance]
            [rdf-path-examples.prefixes :refer [xsd]]
            [clojure.test :refer :all]))

(deftest parse-duration
  (are [s period] (= (distance/parse-duration s) period)
       "P1M" (distance/->period :months 1)
       "P2000Y5M30D" (distance/->period :years 2000 :months 5 :days 30)
       "-P120D" (distance/->period :days -120)
       "P1Y2MT2H" (distance/->period :years 1 :months 2 :hours 2)
       "PT0.5S" (distance/->period :seconds 0.5))
  (is (thrown? IllegalArgumentException (distance/parse-duration ""))
      "Parsing malformed duration throws an exception."))

(deftest date->seconds
  (is (zero? (distance/date->seconds "1970-01-01"))
      "Dates are casted to seconds from the Unix time's start.")
  (is (thrown? IllegalArgumentException (distance/date->seconds ""))
      "Parsing malformed dates throws an exception."))

(deftest compute-distance
  (let [maximum 10 ; Fixed maximum range for all properties
        distance-fn (fn [a b] (distance/compute-distance (fn [])
                                                         {nil maximum}
                                                         [nil a]
                                                         [nil b]))] ; Mocked distance function
    (testing "Equivalent resources have no distance."
      (are [resource] (== (distance-fn resource resource) 0)
           {"@value" 1}
           {"@value" "2015-12-30"}
           {"@value" "https://example.com:3030/path/to/resource"}))
    (testing "Distance is symmetric"
      (are [a b] (= (distance-fn a b) (distance-fn b a))
           {"@value" 1} {"@value" 3}
           {"@value" "http://example.com/path/to/a/file"} {"@value" "https://example.com/path/to/another/file"}
           {"@id" "_:b1"} {"@id" "_:b2"}))
    (testing "Mismatching types have maximum distance."
      (are [a b] (== (distance-fn a b) 1)
           {"@type" (xsd "decimal")
            "@value" 1.23}
           {"@type" "http://purl.org/goodrelations/v1#BusinessEntity"
            "@id" "_:b1"}))
    (testing "Numeric distance"
      (are [a b distance] (== (distance-fn a b) distance)
           {"@value" 0} {"@value" 1} 0.1
           {"@value" -10} {"@value" 5} 1.5))
    (testing "Distance between dates, date-times, and durations."
      (are [a b c d] (= (distance-fn a b) (distance-fn c d))
           ; A day difference
           {"@value" "2015-01-01"} {"@value" "2015-01-02"}
           {"@value" "2014-01-01"} {"@value" "2014-01-02"}
           ; A year difference
           {"@value" "2013-01-01"} {"@value" "2014-01-01"}
           {"@value" "2014-01-01"} {"@value" "2015-01-01"}
           ; A minutes difference
           {"@value" "2015-01-01T12:00:00.000Z"} {"@value" "2015-01-01T12:01:00.000Z"}
           {"@value" "2015-01-01T12:01:00.000Z"} {"@value" "2015-01-01T12:02:00.000Z"}
           ; A month difference
           {"@value" "P5Y"} {"@value" "P4Y11M"}
           {"@value" "P1M"} {"@value" "P2M"}))
    (testing "Malformed literals have maximum distance."
      (are [a b] (== (distance-fn a b) 1)
           {"@type" (xsd "date") "@value" "2016-04-31"} {"@type" (xsd "date") "@value" "2016-04-30"}
           {"@type" (xsd "duration") "@value" "BRRAP"} {"@type" (xsd "duration") "@value" "P5Y"}))))
