(ns rdf-path-examples.type-inference
  (:require [rdf-path-examples.xml-schema :as xsd]
            [rdf-path-examples.prefixes :as prefix]
            [rdf-path-examples.util :refer [duration-regex]]
            [clojure.set :refer [intersection]]
            [clojure.string :as string]))

; ----- Private functions -----

(defn- xml-schema-data-type?
  "Predicate testing if `data-type` is from XML Schema."
  [data-type]
  (zero? (.indexOf data-type (prefix/xsd))))

(defn- data-type->xml-schema
  "Coerce a XML Schema `data-type`."
  [data-type]
  (keyword 'rdf-path-examples.xml-schema (string/replace data-type (prefix/xsd) "")))

(def iri-regex
  ; Copyright (c) 2010-2013 Diego Perini, MIT licensed
  ; https://gist.github.com/dperini/729294
  ; see also https://mathiasbynens.be/demo/url-regex
  ; modified to allow protocol-relative URLs
  ; Taken from <http://stackoverflow.com/a/8317014/385505>
  #"(?i)^(https?|ftp)://(((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:)*@)?(((\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5])\.(\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5])\.(\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5])\.(\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5]))|((([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.)+(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.?)(:\d*)?)(/((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:|@)+(/(([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:|@)*)*)?)?(\?((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:|@)|[\uE000-\uF8FF]|/|\?)*)?(\#((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:|@)|/|\?)*)?$")

(defn infer-datatype
  "Infers data type of `literal` based on matching to regular expressions."
  [^String literal]
  (condp re-matches literal
    #"^\d{4}-\d{2}-\d{2}$" ::xsd/date
    #"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d+)?([+\-]\d{2}:\d{2}|Z)?$" ::xsd/dateTime
    duration-regex ::xsd/duration
    iri-regex ::xsd/anyURI
    ::xsd/string))

(defprotocol Resource
  "An RDF resource"
  (infer-type [resource] "resource type"))

(extend-type PersistentVector
  Resource
  ; We expect the vector's ranges to be homogeneous.
  ; Their data type is inferred from their first member.
  (infer-type [[sample-resource & _]]
    (infer-type sample-resource)))

(extend-type boolean
  Resource
  (infer-type [_] ::xsd/boolean))

(extend-type number
  Resource
  (infer-type [_] ::xsd/decimal))

(extend-type string
  Resource
  (infer-type [value]
    (cond (number? value) ::xsd/decimal
          (or (true? value) (false? value)) ::xsd/boolean
          :else (infer-datatype value))))

(extend-type PersistentArrayMap
  Resource
  (infer-type [{value "@value"
                resource-type "@type"}]
    (cond (nil? value)
          :referent
          (and resource-type (xml-schema-data-type? resource-type))
          (data-type->xml-schema resource-type)
          :else (infer-type value))))

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

(defn is-ordinal?
  "Test if `data-type` is ordinal; e.g., a number."
  [data-type]
  (or (xsd/ordinal-data-types data-type)
      (some (partial xsd/ordinal-data-types)
            (map (partial lowest-common-ancestor data-type) xsd/ordinal-data-types))))
