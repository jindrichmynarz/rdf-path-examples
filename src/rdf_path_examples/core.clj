(ns rdf-path-examples.core
  (:gen-class)
  (:require [rdf-path-examples.resources :refer [rdf-path-examples]]
            [compojure.core :refer [routes ANY]]
            [compojure.route :refer [not-found]]
            [ring.middleware.params :refer [wrap-params]]))

(def app-routes
  (routes 
    (ANY "/generate-examples" [] rdf-path-examples)
    (not-found "Not found")))

(def app
  (wrap-params app-routes))
