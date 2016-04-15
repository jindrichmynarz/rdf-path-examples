(ns rdf-path-examples.clustering-test
  (:require [rdf-path-examples.clustering :as cluster]
            [clojure.set :refer [union]]
            [clojure.test :refer :all]))

(deftest select-k-medoids 
  (letfn [(distance [distances a b]
            ; Mock distance function
            (if (= a b) 0 (get distances (hash-set a b))))
          (k-medoids [distances k]
            ; Wrapper for k-medoids clustering
            (cluster/select-k-medoids (apply union (keys distances)) (partial distance distances) k))]
    (let [distances {#{1 2} 0.5
                     #{1 3} 1
                     #{1 4} 1
                     #{2 3} 0.5
                     #{2 4} 0.5 
                     #{3 4} 0}]
      (= (k-medoids distances 2) #{1 3}))
    (let [distances {#{1 2} 0.5
                     #{1 3} 1
                     #{1 4} 1
                     #{1 5} 0.25
                     #{2 3} 0.5
                     #{2 4} 0.5
                     #{2 5} 0.25
                     #{3 4} 0
                     #{3 5} 0.75
                     #{4 5} 0.75}]
      (= (k-medoids distances 2) #{1 3}))))
