(ns rdf-path-examples.diversity)

; ----- Private functions -----

(defn- min-similarity
  [[p1 s1] [p2 s2]]
  (if (< s1 s2) [p1 s1] [p2 s2]))

; ----- Public functions -----

(defn greedy-construction
  "Select `n` most diverse `paths` using `path-similarities` based on the greedy construction heuristic."
  [^ISet paths
   ^IMap path-similarities
   ^Number n]
  (let [start (rand-nth (vec paths)) ; Randomly select 1 path.
        [solutions] (reduce min-similarity
                            (filter (comp #(contains? % start) first) path-similarities))
        aggregated-similarity (fn [solutions candidate]
                                   [candidate
                                    (transduce (map (comp path-similarities (partial set candidate)))
                                               +
                                               solutions)])
        generate-solution (fn [candidates solutions]
                            (first (reduce min-similarity
                                            (map (partial aggregated-similarity solutions) candidates))))]
    (loop [solutions solutions
           candidates (apply disj paths solutions)
           solutions-count 2]
      (if (= solutions-count n)
        solutions
        (let [solution (generate-solution candidates solutions)]
          (recur (conj solutions solution)
                 (disj candidates solution)
                 (inc solutions-count)))))))
