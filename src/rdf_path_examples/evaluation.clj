(ns rdf-path-examples.evaluation
  (:gen-class)
  (:require [rdf-path-examples.examples :as examples]
            [rdf-path-examples.rdf :refer [json-ld->rdf-model]]
            [rdf-path-examples.util :refer [average try-times]]
            [clojure.java.io :as io]
            [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.pprint :refer [pprint]]
            [clojure.string :as string]
            [clojure.tools.logging :as log])
  (:import [org.apache.jena.rdf.model Model]
           [java.io ByteArrayInputStream]
           [org.apache.commons.math3.stat.inference MannWhitneyUTest]))

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
    (double (/ total-distance
               pairs-count))))

(defn compute-avg-ilds
  "Evaluate paths from `path-dir` using averaged Intra-List Diversity."
  [{:as params}
   ^String path-dir
   & {:keys [number-of-retries
             number-of-runs]
      :or {number-of-retries 5
           number-of-runs 10}}]
  (let [paths (map (juxt #(.getName %) (comp json-ld->rdf-model io/input-stream))
                   (filter #(.isFile %) (file-seq (io/as-file path-dir))))
        path-count (count paths)
        generate-fn (fn [path] (try-times number-of-retries (examples/generate-examples params path)))
        clj->rdf-model (comp json-ld->rdf-model string->input-stream json/generate-string)
        repeatedly-generate-fn (fn [path index]
                                (log/info (str "Processing path " index "/" path-count))
                                (repeatedly number-of-runs
                                            (comp (partial intra-list-diversity params)
                                                  clj->rdf-model
                                                  (partial generate-fn path))))
        avg-ild-fn (fn [[path-name path] index]
                     (let [avg-ild (try (doall (average (repeatedly-generate-fn path index)))
                                        (catch Exception ex
                                          (log/error (.getMessage ex))
                                          :failed))]
                       [path-name avg-ild]))]
    (into {} (map avg-ild-fn paths (range 1 (inc path-count))))))

(defn mann-whitney-u-test
  "Compute Mann-Whitney U test between collections of doubles `a` and `b`."
  [a b]
  (.mannWhitneyUTest (MannWhitneyUTest.) (double-array a) (double-array b)))

(def ^:private
  cli-options
  [["-c" "--config CONFIG" "Path to configuration file in EDN"
    :validate [#(.exists (io/as-file %)) "The configuration file doesn't exist!"]]
   ["-p" "--paths PATHS" "Directory containing RDF paths in JSON-LD"
    :validate [#(.exists (io/as-file %)) "The directory with paths doesn't exist!"]]])

(defn -main
  [& args]
  (let [{{:keys [config paths]} :options
         :keys [errors]} (parse-opts args cli-options)]
    (.addShutdownHook (Runtime/getRuntime) (Thread. shutdown-agents))
    (if (seq errors)
      (do (println errors) (System/exit 1))
      (let [{:keys [number-of-runs]
             :as config} (edn/read-string (slurp config))
            config-name (.getName (io/as-file config))
            avg-ilds (compute-avg-ilds config paths :number-of-runs number-of-runs)
            output-name (str (string/replace config-name #"\.[a-z]+$" "")
                             "_"
                             (java.util.UUID/randomUUID)
                             ".edn")]
        (spit output-name (with-out-str (pprint {:config config :results avg-ilds})))))))
