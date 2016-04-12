(ns rdf-path-examples.diversification-test
  (:require [rdf-path-examples.diversification :as divers]
            [clojure.set :refer [union]]
            [clojure.test :refer :all]))

(deftest greedy-construction
  (let [distances {#{1 2} 0.5
                   #{1 3} 1
                   #{2 3} 0.5}
        paths (apply union (keys distances))]
    (with-redefs [rand-nth (constantly 1)] ; Start with path 1
      (is (= (divers/greedy-construction paths distances 2) #{1 3})))))
