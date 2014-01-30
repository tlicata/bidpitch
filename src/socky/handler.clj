(ns socky.handler
  (:use compojure.core)
  (:require [chord.http-kit :refer [with-channel]]
            [clojure.core.async :refer [<! >! go go-loop put!]]
            [clojure.string :refer [split]]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [hiccup.page :refer [html5 include-js]]
            [hiccup.element :refer [javascript-tag]]
            [org.httpkit.server :as httpkit]
            [socky.crossover.game :as game]))

(defn- include-cljs [path]
  (list
   (javascript-tag "var CLOSURE_NO_DEPS = true;")
   (include-js path)))

(defn page-frame []
  (html5
   [:head
    [:title "HttpKit Example"]
    (include-js "/js/lib/react-0.8.0.js" "/js/bin/main.js")]
   [:body [:div#content]]))

(def game (atom {}))

(defn player-join [name socket]
  (swap! game assoc name {:socket socket})
  (str "thanks for registering, " name))

(defn chat [name message]
  (if-let [player (get @game name)]
    (let [socket (:socket player)]
      (put! socket message)
      (str "thanks for chatting with " name))
    (str "can't send message to " name)))

(defn websocket-handler [request]
  (with-channel request channel
    (go-loop []
     (when-let [{:keys [message]} (<! channel)]
       (println (str "message received: " message))
       (let [[msg val val2] (split message #":")]
         (cond
          (= msg "join") (>! channel (player-join val channel))
          (= msg "bid") (>! channel (str "thanks for " (if (= val "pass") "passing" "bidding")))
          (= msg "play") (>! channel (str "thanks for playing " val))
          (= msg "chat") (>! channel (chat val val2))
          :else (>! channel "unknown message type")))
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
