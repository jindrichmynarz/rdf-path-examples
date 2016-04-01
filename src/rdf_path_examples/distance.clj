(ns rdf-path-examples.distance
  (:require [rdf-path-examples.xml-schema :as xsd]
            [rdf-path-examples.type-inference :refer [data-type->xml-schema]]
            [rdf-path-examples.util :refer [duration-regex parse-number]]
            [clojure.tools.logging :as log])
  (:import [de.lmu.ifi.dbs.elki.distance.distancefunction DistanceFunction]
           [org.joda.time Period]))

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

(defn parse-duration
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

(defmulti ordinal->number
  "Cast ordinal literal as number."
  (fn [datatype literal] (data-type->xml-schema datatype)))

(defmethod ordinal->number :xsd/decimal
  [_ literal]
  (parse-number literal))

(defmethod ordinal->number :xsd/duration
  [_ literal]
  )

(def distance-function
  (reify DistanceFunction
    ;(distance [this])
    (getInputTypeRestriction [this])
    ;(instantiate [this])
    (isMetric [this] false) ; FIXME?
    (isSymmetric [this] true)))

