(defproject bidpitch "0.1.0-SNAPSHOT"
  :description "The card game"
  :url "https://github.com/tlicata/bidpitch"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.9.473"]
                 [org.clojure/core.async "1.3.610"]
                 [org.clojure/core.memoize "1.0.236"]
                 [clj-jwt "0.1.0"]
                 [compojure "1.6.2"]
                 [com.palletops/thread-expr "1.3.0"]
                 [hiccup "1.0.5"]
                 [http-kit "2.5.3"]
                 [jarohen/chord "0.8.1"]
                 [javax.servlet/servlet-api "2.5"]
                 [org.omcljs/om "0.9.0"]]
  :plugins [[lein-cljsbuild "1.1.5"]]
  :hooks [leiningen.cljsbuild]
  :main bidpitch.handler
  :source-paths ["src/clj" "src/cljc"]
  :min-lein-version "2.7.1"
  :profiles {:dev {:dependencies [[ring-mock "0.1.5"]]
                   :plugins [[lein-figwheel "0.5.9"]]
                   :cljsbuild {:builds
                               {:key {:figwheel {:on-jsload "bidpitch.client/on-figwheel-reload"}
                                      :compiler {:asset-path "/cljs"
                                                 :main "bidpitch.client"
                                                 :output-to "target/classes/public/cljs/app.js"
                                                 :output-dir "target/classes/public/cljs"
                                                 :optimizations :none
                                                 :pretty-print true
                                                 :source-map-timestamp true}}}}}
             :prod {:offline? true}
             :uberjar {:aot :all}}
  :uberjar-name "bidpitch-standalone.jar"
  :cljsbuild {:builds
              {:key {:source-paths ["src/cljc" "src/cljs"]
                     :compiler {:output-to "target/classes/public/cljs/app.js"
                                :optimizations :advanced
                                :pretty-print false}}}}
  :figwheel {:css-dirs ["resources/public/css"]})
