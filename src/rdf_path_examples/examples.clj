(ns rdf-path-examples.examples
  (:require [rdf-path-examples.sparql :refer [construct-query describe-query select-query]]
            [rdf-path-examples.json-ld :as json-ld]
            [rdf-path-examples.distance :as distance]
            [rdf-path-examples.diversification :refer [greedy-construction]]
            [rdf-path-examples.util :refer [resource->string]]
            [stencil.core :refer [render-file]]
            [clojure.java.io :as io]
            [clojure.set :refer [union]]
            [yesparql.sparql :refer [model->json-ld]]
            [cheshire.core :as json]
            [clojure.math.combinatorics :refer [combinations]]
            [clojure.tools.logging :as log]
            [clojure.pprint :refer [pprint]])
  (:import [org.apache.jena.rdf.model Model]
           [com.github.jsonldjava.utils JsonUtils]
           [clojure.lang PersistentVector]))

(defonce example-context
  (json-ld/load-context "example.jsonld"))

(defonce extract-path-query
  (resource->string "sparql/extract_path.rq"))

(defonce extract-path-nodes-query
  (resource->string "sparql/extract_path_nodes.rq"))

(defonce extract-datatype-property-ranges-query
  (resource->string "sparql/datatype_property_ranges.rq"))

(defonce extract-examples-query
  (resource->string "sparql/extract_examples.rq"))

(defn extract-vars
  "Extract variable names from `preprocessed-path`."
  [preprocessed-path]
  (sort-by :varname
           (apply union
                  (map (comp set (juxt (comp #(select-keys % [:varname]) :start)
                                       (comp #(select-keys % [:varname :datatype]) :end)))
                       preprocessed-path))))

(defn preprocess-path
  "Preprocess `path` for templating with Mustache."
  [^Model path]
  (let [results (select-query path extract-path-query)
        path (map-indexed (fn [index {:keys [start edgeProperty end isEndDatatype]}]
                            {:start {:first (zero? index)
                                     :type (get start "@id")
                                     :varname (str "e" index)}
                             :edgeProperty (get edgeProperty "@id")
                             :end {:datatype (get isEndDatatype "@value")
                                   :type (get end "@id")
                                   :varname (str "e" (inc index))}})
                          results)]
    {:path path
     :vars (extract-vars path)}))

(defn ^Model retrieve-examples
  "Retrieve examples of `path` using SPARQL query from `query-template-path`."
  [^Model path
   ^String query-template-path
   {:keys [graph-iri limit sparql-endpoint]}]
  (let [path-data (preprocess-path path)
        query (render-file query-template-path
                           (assoc path-data
                                  :graph-iri graph-iri
                                  :limit limit))]
    (construct-query sparql-endpoint query)))

(defn ^Model retrieve-sample-examples
  "Retrieve sample examples of `path` using `sampling-factor`."
  [{:keys [sampling-factor]
    :as params}
   path]
  (retrieve-examples path
                     "sparql/templates/sample_paths.mustache"
                     (update params :limit (partial * sampling-factor))))

(defn extract-examples
  "Extract paths from `examples`."
  [^Model examples]
  (into {} (map (juxt key (comp (partial partition 2) ; Partition into [property node] pairs
                                #(conj % nil) ; Prepend nil before the first node
                                dedupe ; Collapse equal start and end nodes
                                (partial apply concat)
                                (partial map (juxt :start :property :end))
                                val))
                (group-by (comp #(get % "@id") :path)
                          (select-query examples extract-examples-query)))))

(defn extract-path-nodes
  "Extract path nodes from `examples`."
  [^Model examples]
  (map (comp #(get % "@id") :node) (select-query examples extract-path-nodes-query)))

(defn extract-datatype-property-ranges
  "Extracts ranges of datatype properties from `examples`."
  [^Model examples]
  (into {}
        (for [{:keys [property
                      propertyRange]} (select-query examples extract-datatype-property-ranges-query)]
          (try
            [(get property "@id")
             (distance/ordinal->number propertyRange)]
            (catch IllegalArgumentException _ nil)))))

(defn retrieve-path-data
  "Retrieve data describing `path-nodes`."
  [path-nodes
   {:keys [graph-iri sparql-endpoint]}]
  (let [query (render-file "sparql/templates/node_data.mustache"
                           {:graph-iri graph-iri
                            :nodes path-nodes})]
    (construct-query sparql-endpoint query)))

(defn- wrap-types
  "Wraps types of `resource` in array maps"
  [resource]
  (if (contains? resource "@type")
    (update resource "@type" (partial mapv (partial array-map "@id")))
    resource))

(defn find-by-iri
  "Find description of resource identified by `iri` in `json-ld` data."
  [^PersistentVector json-ld
   ^String iri]
  (if-let [matches (filter (comp (partial = iri) #(get % "@id")) json-ld)]
    (-> matches
        first
        (dissoc "@id") ; Remove string @id
        wrap-types)
    {}))

(defn path-distances
  "Materialize distances between paths in `path-map`.
  Nested resources are looked up using `resolve-fn`.
  `datatype-property-ranges` is a map of ranges of datatype properties."
  [path-map resolve-fn datatype-property-ranges]
  (let [distance-fn (partial distance/get-resources-distance resolve-fn datatype-property-ranges)]
    (->> (combinations path-map 2) ; Generate a lazy sequence of all pairs of paths.
         (pmap (juxt (comp set keys) ; Index the results by sets of path IDs
                     (comp distance-fn vals)))
         (into {}))))

(defn describe-paths
  "Retrieve representations of paths identified by `path-iris` from `data`."
  [^Model data
   path-iris]
  (let [query (render-file "sparql/templates/describe_paths.mustache" {:paths path-iris})]
    (describe-query data query)))

(defn retrieve-chosen-paths
  "Retrieve chosen path examples identified by `path-iris` from `data`."
  [^Model data
   path-iris]
  (let [query (render-file "sparql/templates/path_node_labels.mustache" {:paths path-iris})]
    (.union (describe-paths data path-iris) (construct-query data query))))

(defn serialize-examples
  "Serialize path `examples` into JSON-LD Clojure hash-map."
  [^Model examples]
  (if (.isEmpty examples)
    {}
    (-> examples
        model->json-ld
        JsonUtils/fromString
        (json-ld/compact example-context)
        JsonUtils/toString
        json/parse-string)))

(defmulti generate-examples
  "Generate examples of RDF paths using the chosen selection method."
  (fn [config _] (:selection-method config)))

(defmethod generate-examples "random"
  [params
   ^Model path]
  (serialize-examples (retrieve-examples path "sparql/templates/random.mustache" params)))

(defmethod generate-examples "distinct"
  [{:keys [limit] :as params}
   ^Model path]
  (let [examples (retrieve-sample-examples params path)
        path-map (extract-examples examples)
        path-data (retrieve-path-data (extract-path-nodes examples) params)
        path-json-ld (json-ld/expand-model path-data)
        resolve-fn (partial find-by-iri path-json-ld)
        datatype-property-ranges (extract-datatype-property-ranges path-data)
        distances (path-distances path-map resolve-fn datatype-property-ranges)
        chosen-path-iris (vec (greedy-construction (set (keys path-map)) distances limit))
        chosen-paths (retrieve-chosen-paths (.union examples path-data) chosen-path-iris)]
    (serialize-examples chosen-paths)))

(defmethod generate-examples "representative"
  [{:keys [graph-iri limit sampling-factor sparql-endpoint]}
   ^Model path]
  (let []))
