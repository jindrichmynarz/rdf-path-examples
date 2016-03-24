(ns rdf-path-examples.views
  (:require [liberator.representation :refer [render-map-generic]]))

(defmethod render-map-generic "application/ld+json"
  ; Alias the JSON rendering function for JSON-LD
  [data context]
  ((get-method render-map-generic "application/json") data context))

(defn error
  "Render JSON-LD description of the error."
  [{:keys [error-msg status]}]
  {"@context" "http://www.w3.org/ns/hydra/context.jsonld"
   "@type" "Error"
   "statusCode" status
   "description" error-msg})
