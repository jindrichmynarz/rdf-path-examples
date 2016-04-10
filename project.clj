(defproject rdf-path-examples "0.1.0-SNAPSHOT"
  :description "Generates examples of RDF paths"
  :url "http://github.com/jindrichmynarz/rdf-path-examples"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.clojure/data.csv "0.1.3"]
                 [prismatic/schema "1.0.5"]
                 [schema-contrib "0.1.5"]
                 [instaparse "1.4.1"]
                 [stencil "0.5.0" :exclusions [instaparse]]
                 [clj-fuzzy "0.3.1"]
                 [org.apache.jena/jena-core "3.0.1"]
                 [org.apache.jena/jena-arq "3.0.1"]
                 [com.github.jsonld-java/jsonld-java "0.8.2"]
                 [yesparql "0.3.0"]
                 [compojure "1.5.0"]
                 [liberator "0.14.0"]
                 [joda-time "2.9.1"]
                 [org.clojure/math.combinatorics "0.1.1"]

                 [incanter/incanter-core "1.9.0"]
                 [incanter/incanter-charts "1.9.0"]
                 [de.lmu.ifi.dbs.elki/elki "0.7.1" :exclusions [net.sf.trove4j/trove4j]]]
  :jvm-opts ["-server"]
  :main rdf-path-examples.core
  :min-lein-version "2.0.0"
  :plugins [[lein-ring "0.9.7"]]
  :profiles {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                                  [ring/ring-mock "0.3.0"]
                                  [org.clojure/test.check "0.9.0"]
                                  [pjstadig/humane-test-output "0.7.1"]]
                   :injections [(require 'pjstadig.humane-test-output)
                                (pjstadig.humane-test-output/activate!)]
                   :resource-paths ["test/resources"]}
             :uberjar {:aot :all
                       :omit-source true
                       :uberjar-name "rdf-path-examples.jar"}}
  :resource-paths ["resources"]
  :ring {:handler rdf-path-examples.core/app}
  :source-paths ["src"]
  :test-paths ["test"]
  :test-selectors {:default (complement :integration)
                   :integration :integration
                   :all (constantly true)})
