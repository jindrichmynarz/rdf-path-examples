(ns rdf-path-examples.path-test
  (:require [rdf-path-examples.path :as path]
            [cljs.test :refer-macros [are deftest is testing]] 
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop :include-macros true]
            [clojure.test.check.clojure-test :refer-macros [defspec]]))

(defspec interleave-maintains-length
         100
         (prop/for-all [[properties
                         resources] (gen/bind gen/s-pos-int
                                              (fn [path-length]
                                                (gen/tuple (gen/vector gen/string path-length)
                                                           (gen/vector gen/string (inc path-length)))))]
                       (= (+ (count properties) (count resources))
                          (count (path/interleave-path-example properties resources)))))
