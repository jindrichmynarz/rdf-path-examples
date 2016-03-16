(ns rdf-path-examples.resources
  (:require [rdf-path-examples.schema :refer [Config]]
            [rdf-path-examples.jsonld :as jsonld]
            [rdf-path-examples.integrity-constraints :refer [validate-path]]
            [rdf-path-examples.examples :refer [generate-examples]]
            [schema.coerce :as coerce]
            [clojure.tools.logging :as log]
            [liberator.core :refer [defresource]]))

(def parse-query-params
  (coerce/coercer Config coerce/string-coercion-matcher))

(defresource rdf-path-examples
  :allowed-methods [:post]
  :available-media-types ["application/ld+json"]
  :handle-ok (fn [{{configuration :params} :request
                   :keys [rdf-path]}]
               {:message (generate-examples configuration rdf-path)
                :representation {:media-type "application/ld+json"}})
  :known-content-type? (fn [{{{content-type "content-type"} :headers} :request}]
                         (= content-type "application/ld+json"))
  :malformed? (fn [{{:keys [body query-params]} :request}]
                (let [configuration (parse-query-params query-params)
                      path (jsonld/json-ld->rdf-model body)]
                  (if-let [error (or (:error configuration) (validate-path path))]
                    [true {:message (str error)
                           :representation {:media-type (if (string? error)
                                                          "application/ld+json"
                                                          "text/plain")}}]
                    [false {:request {:params (merge {:limit 5} configuration)}
                            :rdf-path path}]))))
