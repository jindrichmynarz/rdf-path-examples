(ns rdf-path-examples.similarity
  (:require [rdf-path-examples.type-inference :refer [infer-type]] 
            [rdf-path-examples.util :refer [duration-regex log wrap-literal]]
            [rdf-path-examples.xml-schema :as xsd]
            [clojure.set :refer [intersection]]
            [clj-fuzzy.stemmers :refer [porter]]
            [clj-fuzzy.metrics :refer [jaro-winkler]]
            [cljs-time.core :refer [date-time]]
            [cljs-time.format :refer [formatters parse]])
  (:import [goog Uri]))

(declare dispatch-similarity)

(def iri-part-weights
  "Pairs of function and weight to compute similarity of IRIs."
  [[#(.getDomain %) 0.68]
   [(comp str #(.getPort %)) 0.01]
   [#(.getFragment %) 0.01]
   [#(.getPath %) 0.15]
   [#(.getQuery %) 0.02]
   [#(.getScheme %) 0.13]])

(defn average
  "Compute average of numbers in collection `coll`."
  [^ISeq coll]
  (if (seq coll)
    (/ (apply + coll)
       (count coll))
    0))

(defn duration->date-time
  "Cast xsd:duration `duration` as date time."
  [^String duration]
  ; TODO: Promote overflows by subtracting and modding: [js/Infinity 12 31? 24 60]
  (when-let [match (re-matches duration-regex duration)]
    (apply date-time (map js/parseFloat match))))

(defn lowest-common-ancestor
  "Compute lowest common ancestor in the type hierarchy of `a` and `b`."
  [a b]
  (cond (isa? a b) b
        (isa? b a) a
        :else (let [a-anc (conj (set (ancestors a)) a)
                    b-anc (conj (set (ancestors b)) b)
                    ac (intersection a-anc b-anc)]
                (when (seq ac)
                  (apply (partial max-key (comp count ancestors)) ac)))))

(defn merge-matching
  "Merge values of matching keys in maps `a` and `b` into a sequence of vectors."
  [^IMap a
   ^IMap b]
  (for [[k v] a
        :let [e (find b k)]
        :when e]
    [v (val e)]))

(defn normalized-bounded-difference
  "Computes similarity via difference between `a` and `b` normalized by `maximum`."
  [a b & {:keys [maximum]
          :or {maximum 10000}}]
  (min (- 1 (/ (js/Math.abs (- a b))
               maximum)) ; Hard-coded maximum value spread 
       1)) ; Maximum bound is necessary because of the hard-coded maximum that may be surpassed. 

(defn probabilistic-sum
  "Compute probabilistic sum of `scores`."
  [^ISeq scores]
  (reduce (fn [a b] (- (+ a b) (* a b))) scores))

(defn- remove-id
  "Remove @id from `resource`."
  [resource]
  (dissoc resource "@id"))

(defn- wrap-literals
  "Wrap plain literals in @value."
  [resource]
  (into {}
        (for [[k v] resource]
          [k
           (if (vector? v)
             (mapv wrap-literal v)
             (wrap-literal v))])))

(defn- wrap-type
  "Wrap @type in `resource`."
  [resource]
  (if (contains? resource "@type")
    (update-in resource ["@type"] (fn [v] {"@id" v}))
    resource))

(defmulti compute-similarity
  "Compute similarity of resources `a` and `b`. Dispatches on the inferred type of the resources.
  Inferred type of `a` and `b` must match, otherwise 0 similarity is returned."
  (fn [_ ^IMap a ^IMap b]
    (let [a-type (infer-type a)
          b-type (infer-type b)]
      (lowest-common-ancestor a-type b-type))))

(defmethod compute-similarity :referent
  [resolve-resource
   {a-id "@id" :as a}
   {b-id "@id" :as b}]
  ; Comparison by IRI
  (if (not= a-id b-id)
    ; Comparison by value
    (let [find-resource (comp wrap-literals wrap-type remove-id resolve-resource)
          a-desc (find-resource a)
          b-desc (find-resource b)
          matching-properties (merge-matching a-desc b-desc)]
      (if (seq matching-properties)
        (/ (reduce + (map (fn [[a b]] (dispatch-similarity resolve-resource a b)) matching-properties))
           (- (+ (count a-desc) (count b-desc)) (count matching-properties)))
        0))
    1))

(defmethod compute-similarity ::xsd/decimal
  [_
   {a "@value"}
   {b "@value"}]
  (normalized-bounded-difference (js/parseFloat a) (js/parseFloat b)))

(defmethod compute-similarity ::xsd/boolean
  [_
   {a "@value"}
   {b "@value"}]
  (if (= a b) 1 0))

(defmethod compute-similarity ::xsd/date
  [_
   {a "@value"}
   {b "@value"}]
  (let [parse-fn (partial parse (:date formatters))]
    (normalized-bounded-difference (parse-fn a) (parse-fn b) :maximum 10000000)))

(defmethod compute-similarity ::xsd/dateTime
  [_
   {a "@value"}
   {b "@value"}]
  (let [parse-fn (partial parse (:date-time formatters))]
    (normalized-bounded-difference (parse-fn a) (parse-fn b) :maximum 10000000)))

(defmethod compute-similarity ::xsd/duration
  [_
   {a "@value"}
   {b "@value"}]
  (if (= a b) 1 0)) ; TODO

(defmethod compute-similarity ::xsd/anyURI
  [_
   {a "@value"}
   {b "@value"}]
  (let [a-iri (Uri.parse a)
        b-iri (Uri.parse b)]
    (apply + (map (fn [[f weight]]
                    (let [a-part (f a-iri)
                          b-part (f b-iri)]
                      (* weight
                         (if (not-any? empty? [a-part a-part])
                           (jaro-winkler a-part b-part)
                           1))))
                  iri-part-weights))))

(defmethod compute-similarity :literal
  [_
   {a-lang "@language"
    a-value "@value"}
   {b-lang "@language"
    b-value "@value"}]
  (if (every? #{"en"} [a-lang b-lang])
    (jaro-winkler (porter a-value) (porter b-value))
    (jaro-winkler a-value b-value)))

(defmethod compute-similarity :default
  ; If no type matches, compared resources are treated as dissimilar.
  [& _]
  0)

(def compute-similarity'
  (memoize compute-similarity))

(defmulti dispatch-similarity
  "Dispatches similarity computation based on the types of the compared objects.
  There may be either a single object or a collection of objects."
  (fn [_ a b] (set [(type a) (type b)])))

(defmethod dispatch-similarity #{PersistentArrayMap}
  ; Comparing single objects.
  [resolve-fn a b]
  (compute-similarity' resolve-fn a b))

(defmethod dispatch-similarity #{PersistentArrayMap PersistentVector}
  ; Comparing single object and a collection of objects.
  ; Aggregated by maximum.
  [resolve-fn a b]
  (let [[v m] (if (vector? a) [a b] [b a])]
    (max (map (partial compute-similarity' resolve-fn m) v))))

(defmethod dispatch-similarity #{PersistentVector}
  ; Comparing collections of objects.
  ; Computes Cartesian product of similarities and aggregates by maximum.
  [resolve-fn a b]
  (max (for [a' a
             b' b]
         (compute-similarity' resolve-fn a' b'))))

(defn get-path-similarity
  "Get similarity of paths `a` and `b` given the `resolve-resource-fn` that
  finds resource description."
  [^IFn resolve-resource-fn [a b]]
  (average (map (partial dispatch-similarity resolve-resource-fn) a b)))
