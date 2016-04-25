(ns rdf-path-examples.diversification
  (:require [clojure.tools.logging :as log])
  (:import [clojure.lang PersistentHashSet]))

(defn greedy-construction
  "Select `n` most diverse `paths` using `distance-fn` that return distance between 2 paths.
  This approach is based on the greedy construction heuristic."
  [^PersistentHashSet paths
   distance-fn
   ^Number n]
  (let [start (rand-nth (vec paths)) ; Randomly select 1 path.
        solutions #{start
                    (reduce (partial max-key (partial distance-fn start))
                            (disj paths start))}
        aggregated-distance (fn [solutions candidate]
                              [candidate
                               (transduce (map (partial distance-fn candidate)) + solutions)])
        generate-solution (fn [candidates solutions]
                            (first (reduce (partial max-key second)
                                           (map (partial aggregated-distance solutions) candidates))))]
    (loop [solutions solutions
           candidates (apply disj paths solutions)
           solutions-count 2]
      (if (= solutions-count n)
        solutions
        (let [solution (generate-solution candidates solutions)]
          (recur (conj solutions solution)
                 (disj candidates solution)
                 (inc solutions-count)))))))
