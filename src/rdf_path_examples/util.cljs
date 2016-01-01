(ns rdf-path-examples.util
  (:require [cljsjs.mustache :as mustache]))

(enable-console-print!)

(def duration-regex
  "Regular expression matching xsd:duration."
  #"^-?P(\d+Y)?(\d+M)?(\d+D)?T?(\d+H)?(\d+M)?(\d+(\.\d+)?S)?$")

(defn find-by-id
  "Helper function to find a resource identified with `id` in `resources`."
  [resources id & {:keys [keywordized?]
                   :or {keywordized? true}}]
  (let [get-id (if keywordized? :id #(get % "@id"))
        matches (filter (comp (partial = id) get-id) resources)]
    (if (seq matches)
      (first matches)
      {"@id" id})))

(defn resolve-resource
  "Resolve a `resource` by using `data`.
  Returns map or vector of maps."
  [data
   {id "@id"
    :as resource}]
  (cond (string? resource) {"@value" resource}
        (map? resource) (if id (find-by-id data id :keywordized? false) resource)
        (coll? resource) (mapv (partial resolve-resource data) resource)))

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
