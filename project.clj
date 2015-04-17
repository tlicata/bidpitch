(defproject socky "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-3196"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]
                 [org.clojure/java.jdbc "0.3.3"]
                 [compojure "1.1.6"]
                 [com.palletops/thread-expr "1.3.0"]
                 [hiccup "1.0.4"]
                 [http-kit "2.1.16"]
                 [jarohen/chord "0.2.2"]
                 [om "0.5.2"]
                 [postgresql "9.1-901.jdbc4"]
                 [ragtime/ragtime.sql.files "0.3.7"]]
  :plugins [[lein-cljsbuild "0.3.3"]
            [com.keminglabs/cljx "0.3.1"]
            [ragtime/ragtime.lein "0.3.7"]]
  :hooks [leiningen.cljsbuild cljx.hooks]
  :main socky.handler
  :source-paths ["src/clj" "target/generated-src/clj"]
  :profiles {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                                  [ring-mock "0.1.5"]]}}
  :cljsbuild {:builds [{:source-paths ["src/cljs" "target/generated-src/cljs"]
                        :compiler {:output-to "resources/public/js/bin/main.js"
                                   :optimizations :whitespace
                                   :pretty-print true}}]}
  :cljx {:builds [{:source-paths ["src/cljx"]
                   :output-path "target/generated-src/clj"
                   :rules :clj}
                  {:source-paths ["src/cljx"]
                   :output-path "target/generated-src/cljs"
                   :rules :cljs}]}
  :ragtime {:migrations ragtime.sql.files/migrations
            :database "jdbc:postgresql://localhost:5432/bidpitch_dev"})
