(ns rdf-path-examples.runner
  (:require [cljs.test :refer-macros [run-all-tests]]
            [doo.runner :refer-macros [doo-tests]]
            [rdf-path-examples.path-test]
            [rdf-path-examples.sparql-test]))

(doo-tests 'rdf-path-examples.path-test
           'rdf-path-examples.sparql-test)
