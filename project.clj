(defproject bidpitch "0.1.0-SNAPSHOT"
  :description "The card game"
  :url "https://github.com/tlicata/bidpitch"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.7.48"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.clojure/core.memoize "0.5.8"]
                 [clj-jwt "0.1.0"]
                 [compojure "1.3.3"]
                 [com.palletops/thread-expr "1.3.0"]
                 [hiccup "1.0.5"]
                 [http-kit "2.1.16"]
                 [jarohen/chord "0.7.0-SNAPSHOT"]
                 [org.omcljs/om "0.8.8"]]
  :plugins [[lein-cljsbuild "1.0.5"]]
  :hooks [leiningen.cljsbuild]
  :main bidpitch.handler
  :source-paths ["src/clj" "src/cljc"]
  :profiles {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                                  [ring-mock "0.1.5"]]}
             :uberjar {:aot :all
                       :dependencies [[javax.servlet/servlet-api "2.5"]]}}
  :cljsbuild {:builds [{:source-paths ["src/clj" "src/cljc" "src/cljs"]
                        :compiler {:output-to "target/classes/public/cljs/app.js"
                                   :optimizations :advanced
                                   :pretty-print false}}]})
