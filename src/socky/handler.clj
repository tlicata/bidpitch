(ns socky.handler
  (:use compojure.core)
  (:require [cemerick.friend :as friend]
            [cemerick.friend.workflows :as workflows]
            [cemerick.friend.credentials :as creds]
            [chord.http-kit :refer [with-channel]]
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

(def users {"tim" {:username "tim"
                   :password (creds/hash-bcrypt "tim_pass")
                   :roles #{::user}}
            "louise" {:username "louise"
                      :password (creds/hash-bcrypt "louise_pass")
                      :roles #{::user}}})

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
     (if-let [{:keys [message]} (<! channel)]
       (let [[msg val val2] (split message #":")]
         (println (str "message received: " message))
         (cond
          (= msg "join") (>! channel (player-join val channel))
          (= msg "bid") (>! channel (str "thanks for " (if (= val "pass") "passing" "bidding")))
          (= msg "play") (>! channel (str "thanks for playing " val))
          (= msg "chat") (>! channel (chat val val2))
          :else (>! channel "unknown message type"))
         (recur))
       (println (str "channel closed"))))))

(defroutes app-routes
  (GET "/" [] (page-frame)))

(defroutes logged-in-routes
  (GET "/socky" [] websocket-handler))

(defroutes fall-through-routes
  (route/resources "/")
  (route/not-found "Not Found"))

(defroutes all-routes
  app-routes
  (-> logged-in-routes
      (friend/authenticate {:credential-fn (partial creds/bcrypt-credential-fn users)
                            :workflows [(workflows/interactive-form)]}))
  fall-through-routes)

(def app
  (handler/site all-routes))

(defn -main []
  (httpkit/run-server app {:port 8080}))
