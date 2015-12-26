(ns rdf-path-examples.util
  (:require [cljsjs.mustache :as mustache]))

(enable-console-print!)

(defn log
  "Log Clojure `data` as a stringified and pretty-printed JSON."
  [data]
  (println (js/JSON.stringify (clj->js data) nil "  ")))

(defn render-template
  "Render Mustache template with data."
  [template & {:keys [data]}]
  (.render js/Mustache template (clj->js data)))
