(ns rdf-path-examples.util
  (:require [cljsjs.mustache :as mustache]))

(defn render-template
  "Render Mustache template with data."
  [template & {:keys [data]}]
  (.render js/Mustache template (clj->js data)))
