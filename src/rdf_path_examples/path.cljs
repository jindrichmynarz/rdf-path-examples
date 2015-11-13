(ns ^:figwheel-always rdf-path-examples.path
  (:require [rdf-path-examples.schema :refer [Config]]
            [rdf-path-examples.sparql :refer [select-query]]
            [rdf-path-examples.jsonld :refer [compact-jsonld path-context]]
            [schema.core :as s]
            [clojure.set :refer [union]]
            [cljs.core.async :refer [<! >! chan put!]]
            [cljsjs.mustache :as mustache]
            [goog.crypt :refer [byteArrayToHex]])
  (:require-macros [rdf-path-examples.macros :refer [read-file]]
                   [cljs.core.async.macros :refer [go]])
  (:import [goog.crypt Md5]))

; ----- SPARQL query templates -----

(defonce random-query (read-file "random.mustache"))

(defonce random-construct-query (read-file "random_construct.mustache"))

; ----- Namespace prefixes -----

(defn- prefix
  "Builds a function for compact IRIs in the namespace `iri`."
  [iri]
  (partial str iri))

(def ^:private rdfs
  (prefix "http://www.w3.org/2000/01/rdf-schema#"))

; ----- Private functions -----

(def ^:private find+rest
  "Find first item satisfying a predicate and return along with the rest of sequence."
  (juxt (comp first filter) remove))

(defn- update-vals
  "Update values of keys `ks` in map `m` by applying function `f`."
  [m ks f]
  (reduce #(update-in % [%2] f) m ks))

(defn- find-by-id
  "Helper function to find a resource identified with `id` in `resources`."
  [resources id]
  (first (filter (comp (partial = id) :id) resources)))

(defn- datatype?
  "Predicate testing whether resource identified with `id` is a datatype."
  [resources id]
  (= (:type (find-by-id resources id)) (rdfs "Datatype")))

(defn- id->varname
  "Convert `id` to variable name by hashing it with MD5 and prefixing it with 'e'."
  [id]
  (str "e" (byteArrayToHex (.digest (doto (Md5.) (.update id))))))

(defn format-path
  "Format a path step `path` for templating via Mustache.
  `resources` is a list of resources comprising the complete path.
  `index` is the index of the path step."
  [resources index path]
  (update-vals path
               [:start :end]
               (fn [id]
                 (let [{:keys [subclassof] :as resource} (find-by-id resources id)]
                   (assoc resource :datatype (datatype? resources subclassof)
                                   :first (zero? index)
                                   :varname (id->varname id)
                                   :type subclassof)))))

(defn extract-vars
  "Extract variable names from `path-steps`."
  [path-steps]
  (letfn [(varname-datatype [variable] (select-keys variable [:varname :datatype]))]
    (apply union
      (map (comp set (juxt (comp varname-datatype :start) (comp varname-datatype :end)))
           path-steps))))

(defn sort-path
  "Sort `path` by following :start and :end."
  ([path]
   (let [[head tail] (find+rest (comp #(not-any? (comp (partial = %) :end) path) :start) path)]
     (sort-path tail [head])))
  ([path sorted-path]
   (if (seq path)
     (let [current-end (:end (peek sorted-path))
           [head tail] (find+rest (comp (partial = current-end) :start) path)]
       (recur tail (conj sorted-path head)))
     sorted-path)))

(defn extract-path
  "Extract RDF path from `path` serialized in JSON-LD for templating it via Mustache."
  [path]
  (go (let [resources (<! (compact-jsonld path path-context))
            path-steps (->> resources
                            (filter (comp (partial = "Path") :type)) 
                            sort-path
                            (map-indexed (partial format-path resources)))]
        {:path path-steps
         :properties (map :edge path-steps)
         :vars (extract-vars path-steps)})))

(defn interleave-path-example
  "Interleave `resources` from path examples with properties.
  For example, if we have resource [:A, :B, :C] and properties [:p1, :p2],
  then the output is [:A :p1 :B :p2 :C]."
  [properties resources]
  {:pre [(= (count properties) (dec (count resources)))]}
  (cons (first resources) (interleave properties (rest resources))))

(defn render-template
  "Render Mustache template with data."
  [template & {:keys [data]}]
  (.render js/Mustache template (clj->js data)))

(defmulti generate-examples (fn [config path callback] (:selection-method config)))

(defmethod generate-examples "random"
  [{:keys [graph-iri limit sparql-endpoint]
    :or {limit 100}
    :as config}
   path
   callback]
  {:pre [(s/validate Config config)]}
  (go (let [{:keys [properties vars] :as path-data} (<! (extract-path path))
            selector (apply juxt (map (comp keyword :varname) vars))
            interleave-fn (comp (partial interleave-path-example properties) selector)
            query (render-template random-query :data (assoc path-data
                                                             :graph-iri graph-iri
                                                             :limit limit))]
        (callback (map interleave-fn (<! (select-query sparql-endpoint query))))))) 

; ----- Testing -----

(def path-1 (js/JSON.parse (read-file "test/path_1.json")))

(def path-2 (js/JSON.parse (read-file "test/path_2.json")))

#_(go (println (<! (extract-path path-2))))

(def config {:sparql-endpoint "http://lod2-dev.vse.cz:8890/sparql"
             :graph-iri "http://linked.opendata.cz/resource/dataset/vestnikverejnychzakazek.cz"
             :selection-method "random"})

#_(generate-examples config path-1 println)
