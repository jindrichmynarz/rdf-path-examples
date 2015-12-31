(ns rdf-path-examples.similarity
  (:require [rdf-path-examples.type-inference :refer [infer-type]] 
            [rdf-path-examples.util :refer [duration-regex log wrap-literal]]
            [rdf-path-examples.prefixes :refer [xsd]]
            [clj-fuzzy.stemmers :refer [porter]]
            [clj-fuzzy.metrics :refer [jaro-winkler]]
            [cljs-time.core :refer [date-time]]
            [cljs-time.format :refer [formatters parse]])
  (:import [goog Uri]))

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

(defn merge-matching
  "Merge values of matching keys in maps `a` and `b` into a sequence of vectors."
  [^IMap a
   ^IMap b]
  (for [[k v] a
        :let [e (find b k)]
        :when e]
    [v (val e)]))

(defn probabilistic-sum
  "Compute probabilistic sum of `scores`."
  [^ISeq scores]
  (reduce (fn [a b] (- (+ a b) (* a b))) scores))

(defmulti compute-similarity
  "Compute similarity of resources `a` and `b`. Dispatches on the inferred type of the resources.
  Inferred type of `a` and `b` must match, otherwise 0 similarity is returned."
  (fn [_ ^IMap a ^IMap b]
    (let [a-type (infer-type a)
          b-type (infer-type b)]
      (when (= a-type b-type)
        a-type))))

(defmethod compute-similarity :referent
  [find-resource-fn
   {a-id "@id" :as a}
   {b-id "@id" :as b}]
  (let [find+wrap (comp wrap-literal find-resource-fn)]
    (if (not= a-id b-id)
      (average (map (partial compute-similarity find-resource-fn)
                    (merge-matching (find+wrap a) (find+wrap b))))
      1)))

(defmethod compute-similarity (xsd "decimal")
  [_
   {a "@value"}
   {b "@value"}]
  (if (= a b) 1 0)) ; TODO

(defmethod compute-similarity (xsd "boolean")
  [_
   {a "@value"}
   {b "@value"}]
  (if (= a b) 1 0))

(defmethod compute-similarity (xsd "date")
  [_
   {a "@value"}
   {b "@value"}]
  (let [parse-fn (partial parse (:date formatters))
        a-date (parse-fn a)
        b-date (parse-fn b)
        difference (js/Math.abs (- a-date b-date))]
    (if (= a b) 1 0))) ; TODO

(defmethod compute-similarity (xsd "dateTime")
  [_
   {a "@value"}
   {b "@value"}]
  (let [parse-fn (partial parse (:date-time formatters))
        a-date-time (parse-fn a)
        b-date-time (parse-fn b)
        difference (js/Math.abs (- a-date-time b-date-time))])
  0) ; TODO

(defmethod compute-similarity (xsd "duration")
  [_
   {a "@value"}
   {b "@value"}]
  (if (= a b) 1 0)) ; TODO

(defmethod compute-similarity (xsd "anyURI")
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

(defn get-path-similarity
  "Get similarity of paths `a` and `b` given the `find-resource-fn` that
  finds resource description."
  [^IFn find-resource-fn [a b]]
  (average (map (partial compute-similarity' find-resource-fn) a b)))
