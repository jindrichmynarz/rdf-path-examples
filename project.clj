(defproject rdf-path-examples "0.1.0-SNAPSHOT"
  :description "Generates examples of RDF paths"
  :url "http://github.com/jindrichmynarz/rdf-path-examples"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.122"]
                 [org.clojure/core.async "0.2.374"]
                 [cljsjs/mustache "1.1.0-0"]
                 [prismatic/schema "1.0.4"]
                 [cljs-http "0.1.38"]
                 [clj-fuzzy "0.3.1"]
                 [org.clojure/test.check "0.9.0"]]

  :plugins [[lein-cljsbuild "1.1.1"]
            [lein-figwheel "0.5.0-2"]
            [lein-doo "0.1.6-SNAPSHOT"]]

  :source-paths ["src"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :cljsbuild {
    :builds [{:id "dev"
              :source-paths ["src"]
              :figwheel {} ; This is needed for Figwheel to work.
              :compiler {:main rdf-path-examples.core
                         :asset-path "js/compiled/out"
                         :output-to "resources/public/js/compiled/rdf_path_examples.js"
                         :output-dir "resources/public/js/compiled/out"
                         :source-map-timestamp true
                         :externs ["externs/jsonld.js"]
                         :foreign-libs [{:file "resources/jsonld.js"
                                         :provides ["jsonld"]
                                         ;:module-type :commonjs
                                         }]
                         }}
             {:id "min"
              :source-paths ["src"]
              :compiler {:output-to "resources/public/js/compiled/rdf_path_examples.js"
                         :main rdf-path-examples.core
                         :optimizations :advanced
                         :externs ["externs/jsonld.js"]
                         :pretty-print false}}
             {:id "test"
              :source-paths ["src" "test"]
              :compiler {:output-to "target/test.js"
                         :main rdf-path-examples.runner
                         :optimizations :whitespace
                         :externs ["externs/jsonld.js"]
                         :foreign-libs [{:file "resources/jsonld.js"
                                         :provides ["jsonld"]}]
                         :pretty-print true}}]}

  :figwheel {:css-dirs ["resources/public/css"]})
