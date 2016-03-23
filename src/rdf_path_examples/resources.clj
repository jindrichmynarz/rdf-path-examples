(ns rdf-path-examples.resources
  (:require [rdf-path-examples.schema :refer [Config]]
            [rdf-path-examples.jsonld :as jsonld]
            [rdf-path-examples.integrity-constraints :refer [validate-path]]
            [rdf-path-examples.examples :refer [generate-examples]]
            [rdf-path-examples.views :as views]
            [schema.coerce :as coerce]
            [clojure.tools.logging :as log]
            [clojure.walk :refer [keywordize-keys]]
            [liberator.core :refer [defresource]]
            [liberator.representation :refer [render-map-generic]])
  (:import [org.apache.jena.sparql.engine.http QueryExceptionHTTP]))

(def ^:private default-response
  {:representation {:media-type "application/ld+json"}})

(def parse-query-params
  (coerce/coercer Config coerce/string-coercion-matcher))

(defmethod render-map-generic "application/ld+json"
  ; Alias the JSON rendering function for JSON-LD
  [data context]
  ((get-method render-map-generic "application/json") data context))

(defresource rdf-path-examples
  :allowed-methods [:post]
  :available-media-types ["application/ld+json"]
  :handle-exception (fn [{:keys [exception]}]
                      (try
                        (throw exception)
                        (catch QueryExceptionHTTP e
                          (views/error {:status 500
                                        :error-msg (.getMessage e)}))))
  :handle-malformed views/error
  :handle-ok (fn [{{configuration :params} :request
                   :keys [rdf-path]}]
               (generate-examples configuration rdf-path))
  :known-content-type? (fn [{{{content-type "content-type"} :headers} :request}]
                         (= content-type "application/ld+json"))
  :malformed? (fn [{{:keys [body query-params]} :request}]
                (let [configuration (parse-query-params query-params)
                      path (jsonld/json-ld->rdf-model body)]
                  (if-let [error (or (:error configuration) (validate-path path))]
                    [true (assoc default-response :error-msg error)]
                    [false {:request {:params (merge {:limit 5} (keywordize-keys configuration))}
                            :rdf-path path}])))
  :new? (constantly false)
  :respond-with-entity? (constantly true))
