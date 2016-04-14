(ns rdf-path-examples.clustering
  (:require [rdf-path-examples.distance :refer [wrap-distance-function]]
            [clojure.set :refer [difference]]
            [clojure.tools.logging :as log])
  (:import [de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans KMedoidsPAM]
           [de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.initialization KMedoidsInitialization]
           [clojure.lang PersistentHashSet]))

(defn- mapsum
  "Map function `f` over collection `coll` and sum the results."
  [f coll]
  (transduce (map f) + coll))

(defn select-k-medoids
  "Select `k` medoids out of `items` based on the k-medoids clustering."
  [^PersistentHashSet paths
   distance-fn'
   ^Number k]
  (let [distance-fn (fn [a b] (if (= a b) 0 (distance-fn' a b)))
        vj (fn [path]
             (mapsum (fn [i]
                       (/ (distance-fn path i)
                          (mapsum (partial distance-fn i) (disj paths i))))
                     (disj paths path)))
        initial-clusters (into {}
                               (map (comp #(vector % #{}) first)
                                    (take k (sort-by second (map (juxt identity vj) paths)))))
        assign-to-cluster (fn [clusters path]
                            (update clusters
                                    (reduce (partial min-key (partial distance-fn path)) (keys clusters))
                                    conj
                                    path))
        sum-distances (fn [clusters]
                        (mapsum (fn [[medoid members]]
                                  (mapsum (partial distance-fn medoid) members))
                                clusters))
        total-distance (fn [cluster member]
                         (mapsum (partial distance-fn member) cluster))
        find-new-medoid (fn [[medoid members]]
                          (cond ; If the cluster is empty, return its medoid.
                                (empty? members) medoid
                                ; If there is 1 member in the cluster, return the member.
                                (= (count members) 1) (first members) 
                                ; Otherwise select the members with the minimum total distance to the others
                                :else (apply min-key (partial total-distance members) members)))
        recluster (fn [clusters]
                    (log/info clusters)
                    (let [initial-clusters (into {} (map (comp #(vector % #{}) find-new-medoid)
                                                         clusters))]
                      (reduce assign-to-cluster
                              initial-clusters
                              (difference paths (set (keys initial-clusters))))))]
    (loop [clusters (reduce assign-to-cluster
                            initial-clusters
                            (difference paths (set (keys initial-clusters))))]
      (let [new-clusters (recluster clusters)
            distance-sum (sum-distances clusters)
            new-distance-sum (sum-distances new-clusters)]
        (if (= distance-sum new-distance-sum)
          (keys clusters)
          (recur (if (> distance-sum new-distance-sum) new-clusters clusters)))))))

(defn k-medoids-initialization
  [k ids distance-function]
  (reify KMedoidsInitialization
    (chooseInitialMedoids [this k ids distance-function])))

(defn k-medoids-pam
  [paths
   distance-fn
   ^Number k
   & {:keys [maxiter]
      :or {maxiter 100}}]
  (let [distance-function (wrap-distance-function distance-fn)]
    (KMedoidsPAM. distance-function
                  k
                  maxiter
                  (k-medoids-initialization k paths distance-function))))
