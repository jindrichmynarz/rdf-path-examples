(ns rdf-path-examples.views
  (:require [rdf-path-examples.sparql :refer [update-operation]]
            [liberator.representation :refer [render-map-generic]]
            [stencil.core :refer [render-file]]
            [yesparql.sparql :refer [model->json-ld]])
  (:import [org.apache.jena.rdf.model ModelFactory]))

(defmethod render-map-generic "application/ld+json"
  ; Alias the JSON rendering function for JSON-LD
  [data context]
  ((get-method render-map-generic "application/json") data context))

(defn error
  "Render JSON-LD description of the error."
  [{:keys [error-msg status see-also]}]
  (let [template-data {:error-msg error-msg
                       :status status}]
    (model->json-ld
      (if see-also
        (update-operation see-also
                          (render-file "sparql/templates/add_hydra_error.mustache" template-data))
        (update-operation (ModelFactory/createDefaultModel)
                          (render-file "sparql/templates/create_hydra_error.mustache" template-data))))))
