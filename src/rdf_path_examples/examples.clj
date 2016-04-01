(ns rdf-path-examples.examples
  (:require [rdf-path-examples.sparql :refer [construct-query select-query]]
            [rdf-path-examples.json-ld :as json-ld]
            [rdf-path-examples.distance :as distance]
            [rdf-path-examples.util :refer [resource->string]]
            [stencil.core :refer [render-file]]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [clojure.set :refer [union]]
            [yesparql.sparql :refer [model->json-ld]])
  (:import [org.apache.jena.rdf.model Model]
           [com.github.jsonldjava.utils JsonUtils]))

(defonce example-context
  (json-ld/load-context "example.jsonld"))

(defonce extract-path-query
  (resource->string "sparql/extract_path.rq"))

(defonce extract-path-nodes-query
  (resource->string "sparql/extract_path_nodes.rq"))

(defonce extract-datatype-property-ranges-query
  (resource->string "sparql/datatype_property_ranges.rq"))

(defonce get-path-ids-query
  (resource->string "sparql/get_path_ids.rq"))

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
                                     :type start
                                     :varname (str "e" index)}
                             :edgeProperty edgeProperty
                             :end {:datatype isEndDatatype
                                   :type end
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

(defn extract-path-nodes
  "Extract path nodes from `examples`."
  [^Model examples]
  (map :node (select-query examples extract-path-nodes-query)))

(defn extract-datatype-property-ranges
  "Extracts ranges of datatype properties from `examples`."
  [^Model examples]
  (into {}
        (for [{:keys [property
                      propertyRange
                      datatype]} (select-query examples extract-datatype-property-ranges-query)]
          [property (distance/ordinal->number datatype propertyRange)])))

(defn get-path-ids
  "Get path blank node IDs from `examples`."
  [^Model examples]
  (map :path (select-query examples get-path-ids-query)))

(defn ^Model retrieve-path-data
  "Retrieve data describing `path-nodes`."
  [path-nodes
   {:keys [graph-iri sparql-endpoint]}]
  (let [query (render-file "sparql/templates/node_data.mustache"
                           {:graph-iri graph-iri
                            :nodes path-nodes})]
    (construct-query sparql-endpoint query)))

(defn serialize-examples
  "Serialize path `examples` into JSON-LD Clojure hash-map"
  [^Model examples]
  (if (.isEmpty examples)
    {}
    (into {} (-> examples
                 model->json-ld
                 JsonUtils/fromString
                 (json-ld/compact example-context)))))

(defmulti generate-examples
  "Generate examples of RDF paths using the chosen selection method."
  (fn [config _] (:selection-method config)))

(defmethod generate-examples "random"
  [params
   ^Model path]
  (serialize-examples (retrieve-examples path "sparql/templates/random.mustache" params)))

(defmethod generate-examples "distinct"
  [{:keys [sampling-factor] :as params}
   ^Model path]
  (let [examples (retrieve-examples path
                                    "sparql/templates/distinct.mustache"
                                    (update params :limit (partial * sampling-factor)))
        path-data (retrieve-path-data (extract-path-nodes examples) params)
        datatype-property-ranges (extract-datatype-property-ranges path-data)]))

(defmethod generate-examples "representative"
  [{:keys [graph-iri limit sampling-factor sparql-endpoint]}
   ^Model path])
