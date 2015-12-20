(ns ^:figwheel-always rdf-path-examples.path
  (:require [rdf-path-examples.schema :refer [Config PathGraph]]
            [rdf-path-examples.sparql :refer [construct-query]]
            [rdf-path-examples.jsonld :refer [compact-jsonld example-context path-context]]
            [rdf-path-examples.prefixes :refer [rdfs]]
            [schema.core :as s]
            [clojure.set :refer [union]]
            [cljs.core.async :refer [<!]]
            [cljsjs.mustache :as mustache])
  (:require-macros [rdf-path-examples.macros :refer [read-file]]
                   [cljs.core.async.macros :refer [go]]))

(enable-console-print!)

; ----- SPARQL query templates -----

(defonce random-query (read-file "random.mustache"))

(defn update-vals
  "Update values of keys `ks` in map `m` by applying function `f`."
  [m ks f]
  (reduce #(update-in % [%2] (partial f %2)) m ks))

(defn find-by-id
  "Helper function to find a resource identified with `id` in `resources`."
  [resources id]
  (first (filter (comp (partial = id) :id) resources)))

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

(defn format-path
  "Format RDF path from `path` serialized in JSON-LD for templating it via Mustache."
  [path]
  (go (let [compact-path (-> path
                             (compact-jsonld path-context)
                             <!
                             (js->clj :keywordize-keys true)
                             :graph) 
            _ (s/validate PathGraph compact-path)
            path-edges (->> compact-path
                            (filter (comp (partial = "Path") :type))
                            first
                            :edges
                            (map-indexed (partial format-edge compact-path)))]
        {:path path-edges
         :vars (extract-vars path-edges)})))

(defn render-template
  "Render Mustache template with data."
  [template & {:keys [data]}]
  (.render js/Mustache template (clj->js data)))

(defmulti generate-examples (fn [config path callback] (:selection-method config)))

(defmethod generate-examples "random"
  [{:keys [graph-iri limit sparql-endpoint]
    :or {limit 5}
    :as config}
   path
   callback]
  {:pre [(s/validate Config config)]}
  (go (let [query (render-template random-query :data (assoc (<! (format-path path))
                                                             :graph-iri graph-iri
                                                             :limit limit))
            query-results (clj->js (<! (construct-query sparql-endpoint query)))
            compact-results (<! (compact-jsonld query-results example-context))]
        (callback compact-results))))

; ----- Testing -----

(def path-1 (js/JSON.parse (read-file "test/path_1.json")))

(def path-2 (js/JSON.parse (read-file "test/path_2.json")))

(def path-3 (js/JSON.parse (read-file "test/path_3.json")))

(def path-4 (js/JSON.parse (read-file "test/path_4.json")))

#_(go (println (<! (format-path path-4))))

#_(go (let [compact-path (-> path-4
                           (compact-jsonld path-context)
                           <!
                           (js->clj :keywordize-keys true)
                           :graph)]
      (println (s/check PathGraph compact-path))))

(def config {:sparql-endpoint "http://lod2-dev.vse.cz:8890/sparql"
             :graph-iri "http://linked.opendata.cz/resource/dataset/vestnikverejnychzakazek.cz"
             :selection-method "random"})

#_(def config {:sparql-endpoint "http://xtest.lmcloud.vse.cz/virtuoso-dbquiz/sparql"
             :graph-iri "http://dbpedia.org/db-quiz"
             :selection-method "random"})

#_(generate-examples config path-1 (comp println js/JSON.stringify))
