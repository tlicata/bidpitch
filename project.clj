(defproject socky "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-3196"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.clojure/java.jdbc "0.3.3"]
                 [compojure "1.3.3"]
                 [com.palletops/thread-expr "1.3.0"]
                 [hiccup "1.0.5"]
                 [http-kit "2.1.16"]
                 [jarohen/chord "0.2.2"]
                 [org.omcljs/om "0.8.8"]
                 [postgresql "9.1-901.jdbc4"]
                 [ragtime/ragtime.sql.files "0.3.7"]]
  :plugins [[lein-cljsbuild "1.0.5"]
            [ragtime/ragtime.lein "0.3.7"]]
  :hooks [leiningen.cljsbuild]
  :main socky.handler
  :source-paths ["src/clj" "target/generated-src/clj"]
  :prep-tasks [["cljx" "once"] "javac" "compile"]
  :profiles {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                                  [ring-mock "0.1.5"]]
                   :plugins [[com.keminglabs/cljx "0.6.0"]]}
             :uberjar {:main socky.handler
                       :aot :all
                       :dependencies [[javax.servlet/servlet-api "2.5"]]}}
  :cljsbuild {:builds [{:source-paths ["src/clj" "src/cljs" "target/generated-src/cljs"]
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
