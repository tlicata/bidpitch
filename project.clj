(defproject bidpitch "0.1.0-SNAPSHOT"
  :description "The card game"
  :url "https://github.com/tlicata/bidpitch"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.473"]
                 [org.clojure/core.async "0.2.395"]
                 [org.clojure/core.logic "0.8.11"]
                 [org.clojure/core.memoize "0.5.8"]
                 [clj-jwt "0.1.0"]
                 [compojure "1.5.2"]
                 [com.palletops/thread-expr "1.3.0"]
                 [hiccup "1.0.5"]
                 [http-kit "2.2.0"]
                 [jarohen/chord "0.7.0"]
                 [org.omcljs/om "0.9.0"]]
  :plugins [[lein-cljsbuild "1.1.5"]]
  :hooks [leiningen.cljsbuild]
  :main bidpitch.handler
  :source-paths ["src/clj" "src/cljc"]
  :profiles {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                                  [ring-mock "0.1.5"]]
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
             :uberjar {:aot :all
                       :dependencies [[javax.servlet/servlet-api "2.5"]]}}
  :cljsbuild {:builds
              {:key {:source-paths ["src/cljc" "src/cljs"]
                     :compiler {:output-to "target/classes/public/cljs/app.js"
                                :optimizations :advanced
                                :pretty-print false}}}}
  :figwheel {:css-dirs ["resources/public/css"]})
