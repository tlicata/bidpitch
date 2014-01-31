(defproject socky "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-2138"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]
                 [compojure "1.1.6"]
                 [hiccup "1.0.4"]
                 [http-kit "2.1.16"]
                 [jarohen/chord "0.2.2"]
                 [om "0.2.3"]]
  :plugins [[lein-ring "0.8.10"]
            [lein-cljsbuild "0.3.3"]]
  :hooks [leiningen.cljsbuild]
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring-mock "0.1.5"]]}}
  :cljsbuild {:builds [{:source-paths ["cljs"]
                        :compiler {:output-to "resources/public/js/bin/main.js"
                                   :optimizations :whitespace
                                   :pretty-print true}}]})
