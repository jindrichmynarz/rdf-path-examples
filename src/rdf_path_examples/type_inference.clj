(ns rdf-path-examples.type-inference
  (:require [rdf-path-examples.xml-schema :as xsd]
            [rdf-path-examples.prefixes :as prefix]
            [rdf-path-examples.util :refer [duration-regex]]
            [clojure.set :refer [intersection]]
            [clojure.string :as string])
  (:import [java.net URI URISyntaxException]
           [clojure.lang PersistentArrayMap PersistentVector]))

(defn xml-schema-data-type?
  "Predicate testing if `data-type` is from XML Schema."
  [^String data-type]
  (.startsWith data-type (prefix/xsd)))

(defn data-type->xml-schema
  "Coerce a XML Schema `data-type`."
  [^String data-type]
  (keyword "rdf-path-examples.xml-schema" (string/replace data-type (prefix/xsd) "")))

(defn absolute-uri?
  "Test if `s` is a valid absolute URI."
  [s]
  (try 
    (.isAbsolute (URI. s))
    (catch URISyntaxException _ false)))

(defn infer-datatype
  "Infers data type of `literal`."
  [literal]
  (cond (instance? Boolean literal) ::xsd/boolean
        (number? literal) ::xsd/decimal
        (absolute-uri? literal) ::xsd/anyURI
        :else (condp re-matches literal
                #"^\d{4}-\d{2}-\d{2}$" ::xsd/date
                #"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d+)?([+\-]\d{2}:\d{2}|Z)?$" ::xsd/dateTime
                duration-regex ::xsd/duration
                ::xsd/string)))

(defprotocol Resource
  "An RDF resource"
  (infer-type [resource] "resource type"))

(extend-protocol Resource
  PersistentArrayMap
  (infer-type [{id "@id"
                value "@value"
                resource-type "@type"}]
    (cond id
          :referent
          (and resource-type (xml-schema-data-type? resource-type))
          (data-type->xml-schema resource-type)
          :else (infer-datatype value)))

  PersistentVector
  ; We expect the vector's ranges to be homogeneous.
  ; Their data type is inferred from their first member.
  (infer-type [[sample-resource & _]]
    (infer-type sample-resource)))

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
