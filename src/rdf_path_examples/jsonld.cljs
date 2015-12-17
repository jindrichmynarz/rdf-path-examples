(ns rdf-path-examples.jsonld
  (:require [cljs.core.async :refer [<! >! chan put!]]
            [jsonld])
  (:require-macros [rdf-path-examples.macros :refer [read-file]]))

; ----- Public vars -----

(def path-context (js/JSON.parse (read-file "contexts/path.json")))

(def example-context (js/JSON.parse (read-file "contexts/example.json")))

; ----- Public functions -----

(defn compact-jsonld
  "Compact `json` serialized in JSON-LD using `context`."
  [json context]
  (let [out (chan 1)]
    (js/jsonld.compact json
                       context
                       (fn [err compacted] (put! out compacted)))
    out))
