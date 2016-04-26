(defproject rdf-path-examples "0.5.0-SNAPSHOT"
  :description "Generates examples of RDF paths"
  :url "http://github.com/jindrichmynarz/rdf-path-examples"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.clojure/data.csv "0.1.3"]
                 [prismatic/schema "1.1.0"]
                 [schema-contrib "0.1.5"]
                 [instaparse "1.4.1"]
                 [stencil "0.5.0" :exclusions [instaparse]]
                 [clj-fuzzy "0.3.1"]
                 [org.apache.jena/jena-core "3.0.1"]
                 [org.apache.jena/jena-arq "3.0.1"]
                 [com.github.jsonld-java/jsonld-java "0.8.2"]
                 [yesparql "0.3.0"]
                 [compojure "1.5.0"]
                 [liberator "0.14.1"]
                 [clj-time "0.11.0"]
                 [joda-time "2.9.3"]
                 [org.clojure/math.combinatorics "0.1.1"]
                 [cheshire "5.6.0"]
                 [incanter/incanter-core "1.9.0"]
                 [incanter/incanter-charts "1.9.0"]
                 [org.apache.commons/commons-math3 "3.0"]
                 [org.clojure/tools.cli "0.3.3"]]
  :jvm-opts ["-server"]
  :main rdf-path-examples.evaluation
  :min-lein-version "2.0.0"
  :plugins [[lein-ring "0.9.7"]]
  :profiles {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                                  [ring/ring-mock "0.3.0"]
                                  [org.clojure/test.check "0.9.0"]
                                  [pjstadig/humane-test-output "0.8.0"]]
                   :injections [(require 'pjstadig.humane-test-output)
                                (pjstadig.humane-test-output/activate!)]
                   :resource-paths ["test/resources"]}
             :uberjar {:aot :all
                       :omit-source true}}
  :resource-paths ["resources"]
  :ring {:handler rdf-path-examples.core/app
         :uberwar-name "rdf-path-examples.war"}
  :source-paths ["src"]
  :test-paths ["test"]
  :test-selectors {:default (complement :integration)
                   :integration :integration
                   :all (constantly true)})
