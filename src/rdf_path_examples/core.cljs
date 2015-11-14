(ns rdf-path-examples.core
  (:require [rdf-path-examples.path :as rdf-path]))

(enable-console-print!)

(defn ^:export generate-examples
  "Generates examples of RDF path described in JSON-LD by `path`.
  Examples are retrieved from the SPARQL endpoint using the method specified in `config`.
  List of example instantiations of the path is provided to the `callback`."
  [config path callback]
  {:pre [(object? config)
         (object? path)
         (fn? callback)]}
  (rdf-path/generate-examples (js->clj config :keywordize-keys true)
                              path
                              (comp callback clj->js)))
