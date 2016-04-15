(ns rdf-path-examples.evaluation
  (:require [rdf-path-examples.examples :as examples]
            [rdf-path-examples.rdf :refer [json-ld->rdf-model]]
            [rdf-path-examples.util :refer [average]]
            [clojure.java.io :as io]
            [cheshire.core :as json]
            [clojure.tools.logging :as log])
  (:import [org.apache.jena.rdf.model Model]
           [java.io ByteArrayInputStream]))

(defn- count-pairs
  "Count number of pairs from `n` items."
  [^Number n]
  (/ (* n (dec n)) 2))

(defn- string->input-stream
  "Convert string `s` as an InputStream."
  [s]
  (ByteArrayInputStream. (.getBytes s)))

(defn intra-list-diversity
  "Compute intra-list diversity as a total pairwise distance of examples divided by the number of pairs."
  [{:keys [limit] :as params}
   ^Model examples]
  (let [path-map (examples/extract-examples examples)
        path-nodes (examples/extract-path-nodes examples)
        path-data (examples/retrieve-path-data path-nodes params)
        distance-fn (examples/get-distance-fn path-map path-data)
        total-distance (reduce + (vals (examples/get-distances path-map path-data)))
        pairs-count (count-pairs limit)]
    (/ total-distance
       pairs-count)))

(defn compute-avg-ild
  "Evaluate paths from `path-dir` using averaged Intra-List Diversity."
  [{:as params}
   ^String path-dir
   & {:keys [number-of-runs]
      :or {number-of-runs 2}}] ; FIXME: Increase the default after testing.
  (let [paths (map (comp json-ld->rdf-model io/input-stream)
                   (filter #(.isFile %) (file-seq (io/as-file (io/resource path-dir)))))
        generate-fn (partial examples/generate-examples params)
        clj->rdf-model (comp json-ld->rdf-model string->input-stream json/generate-string)]
    (doall (map (comp average
                      (fn [path] (repeatedly number-of-runs
                                             (comp (partial intra-list-diversity params)
                                                   clj->rdf-model
                                                   (partial generate-fn path)))))
                paths))))

(comment
  (def config {:limit 5
               :sampling-factor 20
               :selection-method ::examples/distinct
               :sparql-endpoint "http://lod2-dev.vse.cz:4004/vvz/query"})
  (def results (compute-avg-ild config "paths/vvz"))
  )
