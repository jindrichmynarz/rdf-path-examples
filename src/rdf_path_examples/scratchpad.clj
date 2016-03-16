(ns rdf-path-examples.scratchpad
  (:require [clojure.java.io :as io]
            [incanter.core :as incanter]
            [incanter.charts :as charts]
            [clojure.data.csv :as csv])
  (:import [clojure.lang PersistentHashSet]
           [java.awt Color]))

(defn- square
  [n]
  (* n n))

(defn- mapsum
  "Map function `f` over collection `coll` and sum the results."
  [f coll]
  (reduce + (map f coll)))

(defn euclidean-distance
  "Compute Euclidean distance between points [ax ay] and [bx by]."
  [[ax ay] [bx by]]
  (Math/sqrt (+ (square (- ax bx))
                (square (- ay by)))))

(def euclidean-distance'
  (memoize euclidean-distance))

(defn random-points
  "Create an infinite lazy sequence of random [x, y] points from [0, maximum)."
  [& {:keys [maximum]
      :or {maximum 100}}]
  (distinct (repeatedly #(repeatedly 2 (comp (partial * maximum) rand)))))

(defn plot-points
  "Plots XY `points` and highlighted `selected-points` to a scatter plot."
  [points selected-points]
  (let [plot (-> (charts/scatter-plot (map first points)
                                  (map second points)
                                  :x-label ""
                                  :y-label "")
                 charts/clear-background
                 (charts/set-alpha 0.7))]
    (doto plot
      (charts/add-points (map first selected-points)
                         (map second selected-points)))
    (doto (.getPlot plot)
      (.. (getRenderer 0) (setSeriesPaint 0 Color/darkGray))
      (.. (getRenderer 1) (setSeriesPaint 0 Color/red))
      (.. getDomainAxis (setVisible false))
      (.. getRangeAxis (setVisible false)))
    plot))

(defn random-selection
  "Select a random sequence having `n` items from collection `items`."
  [n items]
  (take n (shuffle items)))

(defn distinct-selection
  "Select `n` most diverse items out of `items` based on the greedy construction heuristic."
  [n
   ^PersistentHashSet items]
  (let [start (first (shuffle items))
        aggregated-distance (fn [solutions candidate]
                              (reduce + (map (partial euclidean-distance' candidate) solutions)))
        generate-solution (fn [candidates solutions]
                            (apply max-key (partial aggregated-distance solutions) candidates))]
    (loop [solutions [start]
           candidates (disj items start)
           solutions-count 1]
      (if (= solutions-count n)
        solutions
        (let [solution (generate-solution candidates solutions)]
          (recur (conj solutions solution)
                 (disj candidates solution)
                 (inc solutions-count)))))))

(defn representative-selection
  "Select `n` most representative items out of `items` based on the k-medoids clustering."
  [n 
   ^PersistentHashSet items]
  (let [vj (fn [item]
             (mapsum (fn [i]
                       (/ (euclidean-distance' item i)
                          (mapsum (partial euclidean-distance' i) (disj items i))))
                     (disj items item)))
        initial-clusters (into {}
                               (map (comp #(vector % #{}) first)
                                    (take n (sort-by second (map (juxt identity vj) items)))))
        assign-to-cluster (fn [clusters item]
                            (update clusters
                                    (apply min-key (partial euclidean-distance' item) (keys clusters))
                                    conj
                                    item))
        sum-distances (fn [clusters]
                        (mapsum (fn [[medoid members]]
                                  (mapsum (partial euclidean-distance' medoid) members))
                                clusters))
        total-distance (fn [cluster member]
                         (mapsum (partial euclidean-distance' member) cluster))
        find-new-medoid (fn [cluster]
                          (apply min-key (partial total-distance cluster) cluster))
        recluster (fn [clusters]
                    (let [initial-clusters (into {} (map (comp #(vector % #{}) find-new-medoid second)
                                                         clusters))]
                      (reduce assign-to-cluster initial-clusters items)))]
    (loop [clusters (reduce assign-to-cluster initial-clusters items)]
      (let [new-clusters (recluster clusters)
            distance-sum (sum-distances clusters)
            new-distance-sum (sum-distances new-clusters)]
        (if (= distance-sum new-distance-sum)
          (keys clusters)
          (recur (if (> distance-sum new-distance-sum) new-clusters clusters)))))))

(comment
  (def points
    (with-open [in-file (io/reader (io/resource "mouse.csv"))]
      (set (map (partial map (comp (partial * 100) #(Double. %))) (doall (csv/read-csv in-file))))))

  (incanter/view (plot-points points (random-selection 5 points))); "random_selection.png")
  (incanter/save (plot-points points (distinct-selection 5 points)) "distinct_selection.png")
  (incanter/save (plot-points points (representative-selection 5 points)) "representative_selection.png")
  )
