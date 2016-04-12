(ns rdf-path-examples.diversification)

(def ^:private max-distance
  "Select path with maximum distance.
  Arguments are vectors, where the second item is the distance."
  (partial max-key second))

(defn greedy-construction
  "Select `n` most diverse `paths` using `distances` between them
  based on the greedy construction heuristic."
  [paths
   distances
   ^Number n]
  (let [start (rand-nth (vec paths)) ; Randomly select 1 path.
        [solutions] (reduce max-distance (filter (comp #(contains? % start) key) distances))
        aggregated-distance (fn [solutions candidate]
                              [candidate
                               (transduce (map (comp distances (partial hash-set candidate))) + solutions)])
        generate-solution (fn [candidates solutions]
                            (first (reduce max-distance
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
