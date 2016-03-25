(ns rdf-path-examples.views
  (:require [liberator.representation :refer [render-map-generic]]))

(defmethod render-map-generic "application/ld+json"
  ; Alias the JSON rendering function for JSON-LD
  [data context]
  ((get-method render-map-generic "application/json") data context))

(defn error
  "Render JSON-LD description of the error."
  [{:keys [error-msg status see-also]}]
  (let [hydra-error (cond-> {"@type" "Error"
                             "statusCode" status
                             "description" error-msg}
                      see-also
                      (assoc "rdfs:seeAlso" see-also))
        graph (cond-> [hydra-error] see-also (conj see-also))]
    {"@context" {"@vocab" "http://www.w3.org/ns/hydra/core#"
                 "rdfs" "http://www.w3.org/2000/01/rdf-schema#"}
     "@graph" graph}))
