(ns rdf-path-examples.distance
  (:require [rdf-path-examples.xml-schema :as xsd]
            [rdf-path-examples.type-inference :as infer]
            [rdf-path-examples.util :refer [duration-regex parse-number]]
            [clj-fuzzy.stemmers :refer [porter]]
            [clj-fuzzy.metrics :refer [jaro-winkler]]
            [clojure.string :as string]
            [clj-time.format :as time-format]
            [clj-time.coerce :as time-coerce]
            [clojure.tools.logging :as log])
  (:import [de.lmu.ifi.dbs.elki.distance.distancefunction DistanceFunction]
           [org.joda.time DateTime Period]
           [clojure.lang PersistentArrayMap PersistentVector]))

; ----- Private functions -----

(defn- average
  "Compute average of numbers in collection `coll`."
  [coll]
  (if (seq coll)
    (/ (apply + coll)
       (count coll))
    0))

(defn- trim-last-char
  "Trim last character in string `s`."
  [s]
  (if (string? s)
    (subs s 0 (dec (count s)))
    s))

(defn ^Period ->period
  "Construct a period."
  [& {:keys [years months weeks days hours minutes seconds milliseconds]
      :or {years 0
           months 0
           weeks 0
           days 0
           hours 0
           minutes 0
           seconds 0
           milliseconds 0}}]
  (Period. years months weeks days hours minutes seconds milliseconds))

(defn ^Period parse-duration
  "Parse `duration`"
  [^String duration]
  (when-let [match (re-matches duration-regex duration)]
    (let [change-fn (if (= (first duration) \-) - +)
          [years months days hours minutes seconds] (map (comp change-fn
                                                               (fnil identity 0) ; Guard for nils
                                                               parse-number
                                                               trim-last-char)
                                                         ; First match is the whole matching string,
                                                         ; so we discard it.
                                                         (rest match))]
      (->period :years years
                :months months
                :days days
                :hours hours
                :minutes minutes
                :seconds seconds))))

(defn period->seconds
  "Convert `period` to an approximate number of `seconds`."
  [^Period period]
  (+ (.getSeconds period)
     (* (.getMinutes period) 60)
     (* (.getHours period) 60 60)
     (* (.getDays period) 60 60 24)
     (* (.getMonths period) 60 60 24 30)
     (* (.getYears period) 60 60 24 30 12)))

(defmulti ordinal->number
  "Cast ordinal literal as number."
  (fn [{datatype "@type"
        literal "@value"}]
    (when (and datatype (infer/xml-schema-data-type? datatype))
      (infer/data-type->xml-schema datatype))))

(defmethod ordinal->number ::xsd/duration
  [{literal "@value"}]
  (period->seconds (parse-duration literal)))

(defmethod ordinal->number :default
  [{literal "@value"}]
  literal)

(defn normalized-numeric-distance
  "Computes distance between `a` and `b` normalized by `maximum`."
  [maximum a b]
  (if (= a b)
    0
    (double (/ (Math/abs (- a b))
               (or maximum 10000)))))

(defn ^DateTime parse-date
  "Parse xsd:date `date`."
  [^String date]
  (time-format/parse (time-format/formatters :date) date))

(defn date->seconds
  "Coerce `date` to duration in seconds from Unix time's start."
  [^String date]
  (/ (time-coerce/to-long (parse-date date)) 1000))

(defmulti compute-distance
  "Compute distance of resources `a` and `b`. Dispatches on the lowest common ancestor
  of the inferred types of the compared resources."
  (fn [resolve-fn property-ranges [_ a] [_ b]]
    (let [a-type (infer/infer-type a)
          b-type (infer/infer-type b)]
      (infer/lowest-common-ancestor a-type b-type))))

(defmethod compute-distance ::xsd/decimal
  [_
   property-ranges
   [{property "@id"} {a "@value"}]
   [_ {b "@value"}]]
  (normalized-numeric-distance (get property-ranges property) a b))

(defmethod compute-distance ::xsd/date
  [_
   property-ranges
   [{property "@id"} {a "@value"}]
   [_ {b "@value"}]]
  (normalized-numeric-distance (get property-ranges property)
                               (date->seconds a)
                               (date->seconds b)))

(defmethod compute-distance ::xsd/string
  [_ _
   [_ {a-lang "@language"
       a-value "@value"}]
   [_ {b-lang "@language"
       b-value "@value"}]]
  (let [a-str (str a-value) ; Guard against non-string inputs
        b-str (str b-value)]
    (if (= a-str b-str)
      0
      (if (every? (every-pred (complement nil?) #(string/starts-with? % "en")) [a-lang b-lang])
        (jaro-winkler (porter a-str) (porter b-str))
        (jaro-winkler a-str b-str)))))

(defmethod compute-distance :default
  ; If no type matches, compared resources are treated as dissimilar,
  ; unless they are exactly the same.
  [_ _
   [_ {a "@value"}]
   [_ {b "@value"}]]
  (if (and a b (= a b))
    0
    1))

(def compute-distance'
  (memoize compute-distance))

(defmulti dispatch-distance
  "Dispatches distance computation based on the types of the compared objects.
  There may be either a single object or a collection of objects."
  (fn [distance-fn [_ a] [_ b]] (set (map type [a b]))))

(defmethod dispatch-distance #{PersistentArrayMap}
  ; Comparing single objects.
  [distance-fn a b]
  (distance-fn a b))

(defmethod dispatch-distance #{PersistentArrayMap PersistentVector}
  ; Comparing single object and a collection of objects.
  ; Aggregated by maximum.
  [distance-fn a b]
  (let [[v m] (if (vector? (second a)) [a b] [b a])]
    (max (map (partial distance-fn m) v))))

(defmethod dispatch-distance #{PersistentVector}
  ; Comparing collections of objects.
  ; Computes Cartesian product of similarities and aggregates by maximum.
  [distance-fn [a-property a] [b-property b]]
  (max (for [a' a
             b' b]
         (distance-fn [a-property a'] [b-property b']))))

(defn get-paths-distance
  "Get similarity of paths `a` and `b` given the `resolve-fn` that
  finds resource description and `property-ranges` lists ranges for the
  properties in data."
  [resolve-fn property-ranges [a b]]
  (let [distance-fn (partial compute-distance' resolve-fn property-ranges)
        dispatch-fn (partial dispatch-distance distance-fn)]
    (average (map dispatch-fn a b))))

(def distance-function
  (reify DistanceFunction
    ;(distance [this])
    (getInputTypeRestriction [this])
    ;(instantiate [this])
    (isMetric [this] false) ; FIXME?
    (isSymmetric [this] true)))
