(ns rdf-path-examples.examples
  (:require [rdf-path-examples.sparql :refer [construct-query select-query]]
            [rdf-path-examples.json-ld :as json-ld]
            [stencil.core :refer [render-file]]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [clojure.set :refer [union]]
            [yesparql.sparql :refer [model->json-ld]])
  (:import [org.apache.jena.rdf.model Model]
           [com.github.jsonldjava.utils JsonUtils]))

(defonce example-context
  (json-ld/load-context "example.jsonld"))

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
  (let [query (slurp (io/resource "sparql/extract_path.rq"))
        results (select-query path query)
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
  [{:keys [graph-iri limit sparql-endpoint]}
   ^Model path]
  (let [path-data (preprocess-path path)
        query (render-file "sparql/query_templates/random.mustache"
                           (assoc path-data
                                  :graph-iri graph-iri
                                  :limit limit))
        results (construct-query sparql-endpoint query)]
    (serialize-examples results)))

(defmethod generate-examples "distinct"
  [{:keys [graph-iri limit sparql-endpoint]}
   ^Model path])

(defmethod generate-examples "representative"
  [{:keys [graph-iri limit sparql-endpoint]}
   ^Model path])
