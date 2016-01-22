(ns rdf-path-examples.sparql
  (:require [cljs-http.client :as http]
            [cljs.core.async :refer [chan pipe]]))

; ----- Private functions -----

(defn- sparql-query
  "Execute a SPARQL `query` on `sparql-endpoint` requesting for results in `results-format`
  that will be put on the `results-channel`.
  The query is executed via HTTP POST request to allow for longer queries.
  Request needs to ensure same origin policy or the `endpoint` must enable CORS." 
  [sparql-endpoint query results-format results-channel]
  (pipe (http/post sparql-endpoint
                  {:with-credentials? false ; Necessary for CORS
                   :headers {"accept" results-format}
                   :form-params {:query query
                                 :timeout 100000}})
        results-channel))

(def select-results-channel
  "Creates a channel that extracts values from application/sparql-results+json format."
  (letfn [(extract-values [result] (into {} (map (fn [[k v]] [k (:value v)]) result)))]
    (fn []
      (chan 1 (map (comp (partial map extract-values) :bindings :results :body))))))

; ----- Public functions -----

(defn construct-query
  "Execute a SPARQL CONSTRUCT `query` on `sparql-endpoint`.
  Returns a channel with query results serialized in NTriples."
  ; Virtuoso uses "text/plain" as the MIME type of NTriples.
  ; Fuseki uses the standard MIME type "application/n-triples".
  [sparql-endpoint query]
  (sparql-query sparql-endpoint query "text/plain,application/n-triples" (chan 1 (map :body))))

(defn select-query
  "Execute a SPARQL SELECT `query` on `sparql-endpoint`."
  [sparql-endpoint query]
  (sparql-query sparql-endpoint query "application/sparql-results+json" (select-results-channel)))
