(ns rdf-path-examples.type-inference
  (:require [rdf-path-examples.xml-schema :as xsd]
            [rdf-path-examples.prefixes :as prefix]
            [rdf-path-examples.util :refer [duration-regex]]
            [clojure.string :as string]))

(def iri-regex
  ; Copyright (c) 2010-2013 Diego Perini, MIT licensed
  ; https://gist.github.com/dperini/729294
  ; see also https://mathiasbynens.be/demo/url-regex
  ; modified to allow protocol-relative URLs
  ; Taken from <http://stackoverflow.com/a/8317014/385505>
  #"(?i)^(https?|ftp)://(((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:)*@)?(((\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5])\.(\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5])\.(\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5])\.(\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5]))|((([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.)+(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.?)(:\d*)?)(/((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:|@)+(/(([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:|@)*)*)?)?(\?((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:|@)|[\uE000-\uF8FF]|/|\?)*)?(\#((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:|@)|/|\?)*)?$")

(defn is-ordinal?
  [resource]
  )

(defn infer-datatype
  "Infers data type of `literal` based on matching to regular expressions."
  [^String literal]
  (condp re-matches literal
    #"^\d{4}-\d{2}-\d{2}$" ::xsd/date
    #"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d+)?([+\-]\d{2}:\d{2}|Z)?$" ::xsd/dateTime
    duration-regex ::xsd/duration
    iri-regex ::xsd/anyURI
    :literal))

(defn infer-type
  "Infers a data type of JSON-LD value."
  [{value "@value"
    datatype "@datatype"}]
  (cond (nil? value) :referent
        (and datatype
             (zero? (.indexOf datatype (prefix/xsd)))) (keyword 'rdf-path-examples.xml-schema
                                                                (string/replace datatype (prefix/xsd) ""))
        (number? value) ::xsd/decimal
        (or (true? value) (false? value)) ::xsd/boolean
        :else (infer-datatype value)))
