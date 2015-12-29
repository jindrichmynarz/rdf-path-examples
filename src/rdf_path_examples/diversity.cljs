(ns rdf-path-examples.diversity)

(defn- min-similarity
  [[p1 s1] [p2 s2]]
  (if (< s1 s2) [p1 s1] [p2 s2]))

(defn greedy-construction
  "Select `n` most diverse `paths` using `path-similarities` based on the greedy construction heuristic."
  [paths path-similarities n]
  (let [start (rand-nth (vec paths))
        [solutions] (transduce (filter (comp (partial contains? start) first))
                               min-similarity
                               path-similarities)
        aggregated-similarity (fn [solutions candidate]
                                   [candidate
                                    (transduce (map (comp path-similarities (partial set candidate)))
                                               +
                                               solutions)])
        generate-solution (fn [candidates solutions]
                            (ffirst (transduce (map (partial aggregated-similarity solutions)) 
                                               min-similarity
                                               candidates)))]
    (loop [solutions solutions
           candidates (apply disj paths solutions)
           solutions-count 2]
      (if (= solutions-count n)
        solutions
        (let [solution (generate-solution candidates solutions)]
          (recur (conj solutions solution)
                 (disj candidates solution)
                 (inc solutions-count)))))))
