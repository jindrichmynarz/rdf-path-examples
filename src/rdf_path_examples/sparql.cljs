(ns rdf-path-examples.sparql
  (:require [rdf-path-examples.schema :refer [HTTP-IRI]]
            [schema.core :as s :include-macros true]
            [goog.Uri :as uri]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<! >! chan]])
  (:require-macros [rdf-path-examples.macros :refer [read-file]]
                   [cljs.core.async.macros :refer [go]]))

(defn same-origin?
  "Test if URLs `a` and `b` have the same origin."
  [a b]
  {:pre [(s/validate HTTP-IRI a)
         (s/validate HTTP-IRI b)]}
  (uri/haveSameDomain a b))

(defn sparql-query-channel
  "Virtuoso-specific JSON-P request"
  [endpoint query results-format]
  (http/jsonp endpoint
              {:query-params {; Since JSON-P does not allow setting Accept header,
                              ; we need to use Virtuoso-specific query parameter `format`.
                              :format results-format
                              :query query
                              :timeout 100000}}))

(def select-results-channel
  "Channel that extracts values from application/sparql-results+json format."
  (letfn [(extract-values [result] (into {} (map (fn [[k v]] [k (:value v)]) result)))]
    (fn []
      (chan 1 (map (comp (partial map extract-values) :bindings :results :body))))))

(def construct-results-channel
  (fn [] (chan 1)))

(defn sparql-query
  "Send a SPARQL `query` to `sparql-endpoint`."
  [sparql-endpoint query results-format results-channel]
  (go (>! results-channel (<! (sparql-query-channel sparql-endpoint query results-format))))
  results-channel)

(defn select-query
  "Send a SPARQL SELECT `query` to `sparql-endpoint`."
  [sparql-endpoint query]
  (sparql-query sparql-endpoint query "application/sparql-results+json" (select-results-channel)))

(defn construct-query
  "Send a SPARQL CONSTRUCT `query` to `sparql-endpoint`."
  [sparql-endpoint query]
  (sparql-query sparql-endpoint query "application/x-json+ld" (construct-results-channel)))
