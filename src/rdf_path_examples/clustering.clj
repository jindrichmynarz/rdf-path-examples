(ns rdf-path-examples.clustering
  (:require [clojure.tools.logging :as log])
  (:import [clojure.lang PersistentHashSet]))

(defn- mapsum
  "Map function `f` over collection `coll` and sum the results."
  [f coll]
  (transduce (map f) + coll))

(defn select-k-medoids
  "Select `k` medoids out of `items` based on the k-medoids clustering
  using the distance function `distance-fn`. It uses the algorithm described
  in PARK, Hae-Sang; JUN, Chi-Hyuck, 2009. A simple and fast algorithm for
  K-medoids clustering. Expert Systems with Applications. Vol. 36, pp. 3336–3341.
  DOI 10.1016/j.eswa.2008.01.039."
  [^PersistentHashSet paths
   distance-fn
   ^Number k
   & {:keys [max-iterations]
      :or {max-iterations 100}}]
  (let [; Function for selecting initial medoids
        vj (fn [path]
             (mapsum (fn [i]
                       (/ (distance-fn path i)
                          (mapsum (partial distance-fn i) (disj paths i))))
                     (disj paths path)))
        initial-clusters (into {}
                               (map (comp (juxt identity hash-set) first)
                                    (take k (sort-by second (map (juxt identity vj) paths)))))
        ; Path is assigned to the medoid it is closest to.
        assign-to-cluster (fn [clusters path]
                            (update clusters
                                    (reduce (partial min-key (partial distance-fn path)) (keys clusters))
                                    conj
                                    path))
        ; Compute the total sum of distances of medoids to the members of their clusters.
        sum-distances (fn [clusters]
                        (mapsum (fn [[medoid members]]
                                  (mapsum (partial distance-fn medoid) members))
                                clusters))
        ; Compute the total of distances between the members of a cluster.
        total-distance (fn [cluster member]
                         (mapsum (partial distance-fn member) cluster))
        ; Find new medoid from members of a cluster.
        find-new-medoid (fn [[medoid members]]
                          (if (= (count members) 1)
                            ; If there is 1 member in the cluster, return the member.
                            (first members)
                            ; Otherwise select the members with the minimum total distance to the others
                            (apply min-key (partial total-distance members) members)))
        recluster (fn [clusters]
                    (let [initial-clusters (into {}
                                                 (map (comp (juxt identity hash-set) find-new-medoid)
                                                      clusters))]
                      (reduce assign-to-cluster initial-clusters paths)))]
    (loop [clusters (reduce assign-to-cluster initial-clusters paths)
           iterations 0]
      (if (> iterations max-iterations)
        (do (log/info "Maximum number of iterations reached.")
            (keys clusters))
        (let [new-clusters (recluster clusters)
              distance-sum (sum-distances clusters)
              new-distance-sum (sum-distances new-clusters)]
          (if (= distance-sum new-distance-sum)
            (keys clusters) ; If the total distance stays the same, return the medoids.
            (recur (if (> distance-sum new-distance-sum) new-clusters clusters)
                   (inc iterations))))))))
