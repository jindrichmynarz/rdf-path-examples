(ns rdf-path-examples.util
  (:require [cljsjs.mustache :as mustache]))

(enable-console-print!)

(def duration-regex
  "Regular expression matching xsd:duration."
  #"^-?P(\d+Y)?(\d+M)?(\d+D)?T?(\d+H)?(\d+M)?(\d+(\.\d+)?S)?$")

(defn log
  "Log Clojure `data` as a stringified and pretty-printed JSON."
  [data]
  (println (js/JSON.stringify (clj->js data) nil "  ")))

(defn render-template
  "Render Mustache template with data."
  [template & {:keys [data]}]
  (.render js/Mustache template (clj->js data)))

(defn wrap-literal
  "Wraps a JSON-LD literal in a map."
  [resource]
  (if (map? resource) resource {"@value" resource}))
