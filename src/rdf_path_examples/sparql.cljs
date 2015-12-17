(ns rdf-path-examples.sparql
  (:require [cljs-http.client :as http]
            [cljs.core.async :refer [<! >! chan]])
  (:require-macros [rdf-path-examples.macros :refer [read-file]]
                   [cljs.core.async.macros :refer [go]]))

(defn sparql-query-channel
  "Virtuoso-specific JSON-P request"
  [endpoint query results-format]
  (http/jsonp endpoint
              {:keywordize-keys? false
               :query-params {; Since JSON-P does not allow setting Accept header,
                              ; we need to use Virtuoso-specific query parameter `format`.
                              :format results-format
                              :query query
                              :timeout 100000}}))

(defn sparql-query
  "Send a SPARQL `query` to `sparql-endpoint`."
  [sparql-endpoint query results-format results-channel]
  (go (>! results-channel (<! (sparql-query-channel sparql-endpoint query results-format))))
  results-channel)

(defn construct-query
  "Send a SPARQL CONSTRUCT `query` to `sparql-endpoint` requesting JSON-LD response."
  [sparql-endpoint query]
  (sparql-query sparql-endpoint query "application/x-json+ld" (chan 1 (map :body))))
