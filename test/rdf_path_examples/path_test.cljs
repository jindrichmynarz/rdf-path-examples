(ns rdf-path-examples.path-test
  (:require [rdf-path-examples.path :as path]
            [cljs.test :refer-macros [are deftest is testing]] 
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop :include-macros true]
            [clojure.test.check.clojure-test :refer-macros [defspec]])
  (:require-macros [rdf-path-examples.macros :refer [read-file]]))

(def no-path (js/JSON.parse (read-file "test/no_path.json")))

(deftest validate-path
  (testing "Path validation"
    ;(is (thrown? js/Error (path/not-empty? (js-obj))))
    ;(is (thrown? js/Error (path/has-path? no-path)))
    ))

(deftest decompose-map-test
  (testing "Map decomposition"
    (are [in out] (= (path/decompose-map in) out)
         {:a :b
          :c {:d {:e :f
                  :g :h}
              :i :j}}
         [[:a :b]
          [:c {:d {:e :f
                   :g :h}
               :i :j}]
          [:d {:e :f
               :g :h}]
          [:e :f]
          [:g :h]
          [:i :j]])))
