(ns rdf-path-examples.views)

(defn error
  "Render JSON-LD description of the error."
  [{:keys [error-msg status]}]
  {"@context" "http://www.w3.org/ns/hydra/context.jsonld"
   "@type" "Error"
   "statusCode" status
   "description" error-msg})
