(ns rdf-path-examples.path
  (:require [rdf-path-examples.schema :refer [Config PathGraph]]
            [rdf-path-examples.sparql :refer [construct-query]]
            [rdf-path-examples.jsonld :as jsonld]
            [rdf-path-examples.prefixes :refer [rdfs]]
            [rdf-path-examples.combinations :refer [combinations]]
            [rdf-path-examples.util :refer [render-template]]
            [schema.core :as s]
            [clojure.set :refer [union]]
            [cljs.core.async :refer [<!]])
  (:require-macros [rdf-path-examples.macros :refer [read-file]]
                   [cljs.core.async.macros :refer [go]]))

(def sampling-factor
  "How many times more examples to retrieve for non-random selection methods."
  2) ; FIXME: Increase after testing.

; ----- SPARQL query templates -----

(defonce random-query (read-file "random.mustache"))

(defonce distinct-query (read-file "distinct.mustache"))

(defn update-vals
  "Update values of keys `ks` in map `m` by applying function `f`."
  [m ks f]
  (reduce #(update-in % [%2] (partial f %2)) m ks))

(defn find-by-id
  "Helper function to find a resource identified with `id` in `resources`.
  Returns `id` when resource is not found."
  [resources id & {:keys [keywordized?]
                   :or {keywordized? true}}]
  (let [matches (filter (comp (partial = id) (if keywordized? :id #(get % "@id"))) resources)]
    (if (seq matches)
      (first matches)
      id)))

(def datatype?
  "Predicate testing whether resource identified with `id` is a datatype."
  (comp (partial = (rdfs "Datatype")) :type))

(defn format-edge
  "Formats a path `edge` for templating via Mustache.
  `path` is a list of resources comprising the complete path.
  `index` is the index of the path step."
  [path index edge]
  (update-vals edge
               [:start :end]
               (fn [property vertex]
                 (let [vertex-type (find-by-id path (:type vertex))]
                   (assoc vertex
                          :datatype (datatype? vertex-type)
                          :first (zero? index)
                          :varname (str "e"
                                        (if (= property :start)
                                          index
                                          (inc index)))
                          :type (:id vertex-type))))))

(defn extract-vars
  "Extract variable names from `path-edges`."
  [path-edges]
  (letfn [(varname-datatype [variable] (select-keys variable [:varname :datatype]))]
    (sort-by :varname (apply union
                             (map (comp set (juxt (comp varname-datatype :start)
                                                  (comp varname-datatype :end)))
                                  path-edges)))))

(defn put-path-first
  "Put instance of Path first in the `path-graph`."
  [path-graph]
  (sort-by :type (fn [resource-type] (if (= resource-type "Path") -1 1)) path-graph))

(defn compact-path
  "Apply JSON-LD compaction to make `path` structure regular."
  [path]
  (go (-> path
          (jsonld/compact-jsonld jsonld/path-context)
          <!
          (js->clj :keywordize-keys true)
          :graph
          put-path-first)))

(defn format-path
  "Format RDF path from `compacted-path` serialized in JSON-LD for templating it via Mustache."
  [compacted-path]
  (let [path-edges (->> compacted-path
                        (filter (comp (partial = "Path") :type))
                        first
                        :edges
                        (map-indexed (partial format-edge compacted-path)))]
    {:path path-edges
     :vars (extract-vars path-edges)}))

(defn preprocess-path
  "Preprocess `path` for SPARQL query"
  [path]
  (go (let [compacted-path (<! (compact-path path))]
        (s/validate PathGraph compacted-path)
        (format-path compacted-path))))

(def filter-paths
  "Transducer filtering resources that instantiate Path."
  (filter (comp (partial = "Path") #(get % "@type"))))

(defn extract-edges
  "Transducer that extracts edges of a path, provided the function `find-resource`
  that retrieves resource description via its @id."
  [find-resource]
  (map (juxt #(get % "@id")
             (comp dedupe
                   (partial mapcat (comp (juxt #(get % "start") #(get % "end"))
                                         find-resource))
                   #(get % "edges")))))

(defmulti generate-examples (fn [config path callback] (:selection-method config)))

(defmethod generate-examples "random"
  [{:keys [graph-iri limit sparql-endpoint]
    :or {limit 5}
    :as config}
   path
   callback]
  {:pre [(s/validate Config config)]}
  (go (let [formatted-path (<! (preprocess-path path))
            query (render-template random-query :data (assoc formatted-path
                                                             :graph-iri graph-iri
                                                             :limit limit))
            query-results (<! (construct-query sparql-endpoint query))
            compacted-results (-> query-results
                                  jsonld/rdf->jsonld
                                  <!
                                  (jsonld/compact-jsonld jsonld/example-context)
                                  <!)]
        (callback compacted-results))))

(defmethod generate-examples "distinct"
  [{:keys [graph-iri limit sparql-endpoint]
    :or {limit 5}
    :as config}
   path
   callback]
  (go (let [formatted-path (<! (preprocess-path path))
            query (render-template distinct-query :data (assoc formatted-path
                                                               :graph-iri graph-iri
                                                               :limit (* limit sampling-factor)))
            query-results (<! (construct-query sparql-endpoint query))
            graph (-> query-results
                      jsonld/rdf->jsonld
                      <!
                      (jsonld/compact-jsonld jsonld/example-context)
                      <!
                      js->clj
                      (get "@graph"))
            find-resource (fn [resource] (if (map? resource)
                                           (find-by-id graph (get resource "@id") :keywordized? false)
                                           {"@value" resource}))
            extract-fn (comp filter-paths
                             (extract-edges find-resource))
            path-map (into {} extract-fn graph)
            data (map (partial map find-resource)
                      (apply map
                             (comp set vector)
                             (vals path-map)))]
        (callback data))))
