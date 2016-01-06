(ns rdf-path-examples.path
  (:require [rdf-path-examples.schema :refer [Config PathGraph]]
            [rdf-path-examples.sparql :refer [construct-query select-query]]
            [rdf-path-examples.jsonld :as jsonld]
            [rdf-path-examples.prefixes :refer [rdfs]]
            [rdf-path-examples.combinations :refer [combinations]]
            [rdf-path-examples.util :refer [find-by-id log render-template resolve-resource]]
            [rdf-path-examples.similarity :refer [get-path-similarity]]
            [rdf-path-examples.diversity :refer [greedy-construction]]
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

(defonce node-data-query (read-file "node_data.mustache"))

(defonce property-ranges-query (read-file "property_ranges.mustache"))

(defn update-vals
  "Update values of keys `ks` in map `m` by applying function `f`."
  [m ks f]
  (reduce #(update-in % [%2] (partial f %2)) m ks))

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
  "Put the instance of Path first in the `path-graph`."
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
  "Builds a transducer that extracts edges of a path, provided the `resolve-fn` function
  that retrieves resource description via its @id."
  [resolve-fn]
  (map (juxt #(get % "@id")
             (comp dedupe
                   (partial mapcat
                            (comp (juxt #(get % "start") #(get % "end"))
                                  resolve-fn))
                   #(get % "edges")))))

(defn get-path-data
  "Get path data in JSON-LD formatted as a Clojure data structure."
  [{:keys [graph-iri limit sparql-endpoint]}
   formatted-path]
  (let [query (render-template distinct-query :data (assoc formatted-path
                                                           :graph-iri graph-iri
                                                           :limit (* limit sampling-factor)))]
    (go (-> (construct-query sparql-endpoint query)
            <!
            jsonld/rdf->jsonld
            <!
            (jsonld/compact-jsonld jsonld/example-context)
            <!
            js->clj
            (get "@graph")))))

(defn get-source-data
  "Get source data describing `path-nodes`."
  [{:keys [graph-iri sparql-endpoint]}
   path-nodes]
  (let [query (render-template node-data-query :data {:nodes path-nodes
                                                      :graph-iri graph-iri})]
    (go (let [response (-> (construct-query sparql-endpoint query)
                           <!
                           jsonld/rdf->jsonld
                           <!)]
          (when-not (empty? response)
            (-> response
                (jsonld/compact-jsonld #js {}) ; Compact with empty context to coerce compact representation.
                <!
                js->clj
                (get "@graph")))))))

(defn get-property-ranges
  [{:keys [graph-iri sparql-endpoint]}
   formatted-path]
  (let [path (update-in formatted-path
                        [:vars]
                        (fn [vars]
                          (let [mark-index (- (count vars) 2)]
                            (map-indexed (fn [index item]
                                           (if (= index mark-index)
                                             (assoc item :last true) item))
                                         vars))))
        query (render-template property-ranges-query :data (assoc path :graph-iri graph-iri))]
    (select-query sparql-endpoint query)))

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
            query-results (-> (construct-query sparql-endpoint query)
                              <!
                              jsonld/rdf->jsonld
                              <!)
            result (if (empty? query-results)
                     query-results
                     (-> query-results
                         (jsonld/compact-jsonld jsonld/example-context)
                         <!))]
        (callback result))))

(defmethod generate-examples "distinct"
  [{:keys [limit]
    :or {limit 5}
    :as config}
   path
   callback]
  {:pre [(s/validate Config config)]}
  (go (let [formatted-path (<! (preprocess-path path))
            ; property-ranges (<! (get-property-ranges config formatted-path))
            path-data (<! (get-path-data (assoc config :limit limit) formatted-path))]
        (if-not (empty? path-data)
          (let [find-path-data (partial resolve-resource path-data)
                path-map (into {} (comp filter-paths (extract-edges find-path-data)) path-data)
                paths (set (keys path-map))
                path-nodes (remove nil? (map #(get % "@id") (apply concat (vals path-map))))
                source-data (<! (get-source-data config path-nodes))
                find-source-data (partial resolve-resource source-data)
                ; Similarities are indexed by sets of the compared paths' IDs.
                ; _ (.profile js/console "Compute similarity")
                path-similarities (into {} (map (juxt (comp set keys)
                                                      (comp (partial get-path-similarity find-source-data) vals))
                                                (combinations path-map 2)))
                ; _ (.profileEnd js/console)
                path-example-ids (greedy-construction paths path-similarities limit)
                ; Reconstruct a JSON-LD serialization of path examples
                path-examples (map (comp find-path-data (fn [id] {"@id" id})) path-example-ids)
                path-example-edges (map find-path-data (mapcat #(get % "edges") path-examples))
                path-example-nodes (map find-source-data
                                        (remove nil? (mapcat (comp #(get % "@id") path-map) path-example-ids)))
                results (doto (clj->js {"@graph" (concat path-examples path-example-edges path-example-nodes)})
                          (aset "@context" (aget jsonld/example-context "@context")))]
            (callback results))
          (callback path-data)))))
