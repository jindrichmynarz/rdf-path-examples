(ns rdf-path-examples.similarity
  (:require [rdf-path-examples.type-inference :refer [infer-type]] 
            [rdf-path-examples.util :refer [log wrap-literal]]
            [rdf-path-examples.prefixes :refer [xsd]]
            [clj-fuzzy.stemmers :refer [porter]]
            [clj-fuzzy.metrics :refer [jaro-winkler]]
            [cljs-time.format :refer [formatters parse]])
  (:import [goog Uri]))

(defn average
  "Compute average of numbers in collection `coll`."
  [coll]
  (if (seq coll)
    (/ (apply + coll)
       (count coll))
    0))

(defn probabilistic-sum
  "Compute probabilistic sum of `scores`."
  [scores]
  (reduce (fn [a b] (- (+ a b) (* a b))) scores))

(defn merge-matching
  "Merge values of matching keys in maps `a` and `b` into a sequence of vectors."
  [a b]
  (for [[k v] a
        :let [e (find b k)]
        :when e]
    [v (val e)]))

(defmulti compute-similarity
  "Compute similarity of resources `a` and `b`. Dispatches on the inferred type of the resources.
  Inferred type of `a` and `b` must match, otherwise 0 similarity is returned."
  (fn [_ a b] (let [a-type (infer-type a)
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
  0)

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
    0))

(defmethod compute-similarity (xsd "dateTime")
  [_
   {a "@value"}
   {b "@value"}]
  (let [parse-fn (partial parse (:date-time))
        a-date-time (parse-fn a)
        b-date-time (parse-fn b)
        difference (js/Math.abs (- a-date b-date))])
  0)

(defmethod compute-similarity (xsd "duration")
  [_
   {a "@value"}
   {b "@value"}]
  0)

(defmethod compute-similarity (xsd "anyURI")
  [_
   {a "@value"}
   {b "@value"}]
  (let [a-iri (Uri.parse a)
        b-iri (Uri.parse b)]
    0))
; .getDomain
; .getPort
; .getFragment
; .getPath
; .getQuery
; .getScheme
; .getUserInfo

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
  [find-resource-fn [a b]]
  (average (map (partial compute-similarity' find-resource-fn) a b)))
