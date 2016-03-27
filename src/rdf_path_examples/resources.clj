(ns rdf-path-examples.resources
  (:require [rdf-path-examples.schema :refer [Config]]
            [rdf-path-examples.json-ld :as json-ld]
            [rdf-path-examples.integrity-constraints :refer [validate-path]]
            [rdf-path-examples.examples :refer [generate-examples]]
            [rdf-path-examples.views :as views]
            [schema.coerce :as coerce]
            [clojure.tools.logging :as log]
            [clojure.walk :refer [keywordize-keys]]
            [liberator.core :refer [defresource]])
  (:import [org.apache.jena.sparql.engine.http QueryExceptionHTTP]
           [schema.utils ErrorContainer]))

(def ^:private default-response
  {:representation {:media-type "application/ld+json"}})

(def parse-query-params
  (coerce/coercer Config coerce/string-coercion-matcher))

(defresource rdf-path-examples
  :allowed-methods [:post]
  :available-media-types ["application/ld+json"]
  :handle-exception (fn [{:keys [exception]}]
                      (try
                        (throw exception)
                        (catch QueryExceptionHTTP e
                          (views/error {:status 500
                                        :error-msg (.getMessage e)}))))
  :handle-malformed (fn [{:keys [malformed-error]
                          :as ctx}]
                      (cond (string? malformed-error)
                              (views/error (assoc ctx :error-msg malformed-error))
                            (map? malformed-error)
                              (views/error (assoc ctx
                                                  :error-msg "Integrity constraint violation"
                                                  :see-also malformed-error))))
  :handle-ok (fn [{{configuration :params} :request
                   :keys [rdf-path]}]
               (generate-examples configuration rdf-path))
  :known-content-type? (fn [{{{content-type "content-type"} :headers} :request}]
                         (= content-type "application/ld+json"))
  :malformed? (fn [{{:keys [body query-params]} :request}]
                (let [configuration (parse-query-params query-params)
                      path (json-ld/json-ld->rdf-dataset body)]
                  (if-let [error (or (:error configuration) (validate-path path))]
                    [true (assoc default-response
                                 :malformed-error (if (instance? ErrorContainer configuration)
                                                    (str error)
                                                    error))]
                    [false {:request {:params (merge {:limit 5} (keywordize-keys configuration))}
                            :rdf-path path}])))
  :new? (constantly false)
  :respond-with-entity? (constantly true))
