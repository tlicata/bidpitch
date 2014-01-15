(ns socky.handler
  (:use compojure.core)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [hiccup.page :refer [html5 include-js]]
            [org.httpkit.server :as httpkit]))

(defn page-frame []
  (html5
   [:head
    [:title "HttpKit Example"]
    (include-js "/js/bin/main.js")]
   [:body [:div#content]]))

(defn websocket-handler [request]
  (httpkit/with-channel request channel
    (httpkit/on-close channel (fn [status] (println "channel closed")))
    (if (httpkit/websocket? channel)
      (println "WebSocket channel")
      (println "HTTP channel"))
    (httpkit/on-receive channel
                (fn [data]
                  (println (str "on receive: " data))
                  (httpkit/send! channel data)))))

(defroutes app-routes
  (GET "/" [] (page-frame))
  (GET "/socky" [] websocket-handler)
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (handler/site app-routes))

(defn -main []
  (httpkit/run-server app {:port 8080}))
