(ns socky.handler
  (:use compojure.core)
  (:require [chord.http-kit :refer [with-channel]]
            [clojure.core.async :refer [<! >! go-loop]]
            [compojure.handler :as handler]
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
  (with-channel request channel
    (go-loop []
     (let [{:keys [message]} (<! channel)]
       (println (str "message received: " message))
       (>! channel (str "Hello client from server: " message))
       (recur)))))

(defroutes app-routes
  (GET "/" [] (page-frame))
  (GET "/socky" [] websocket-handler)
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (handler/site app-routes))

(defn -main []
  (httpkit/run-server app {:port 8080}))
