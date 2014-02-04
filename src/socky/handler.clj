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
            [ring.util.response :as resp]
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

(defn page-dummy []
  (html5
   [:head
    [:title "HttpKit Dummy"]]
   [:body [:div#content [:p "Dummy"]]]))

(defn page-login []
  (html5
   [:head
    [:title "Login"]]
   [:body
    [:form {:method "POST" :action "login"}
     [:div "Username" [:input {:type "text" :name "username"}]]
     [:div "Password" [:input {:type "password" :name "password"}]]
     [:div [:input {:type "submit" :class "button" :value "Login"}]]]]))

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
    (when-let [user (friend/current-authentication)]
      (player-join (:username user) channel)
      (go-loop []
        (if-let [{:keys [message]} (<! channel)]
          (let [[msg val val2] (split message #":")]
            (println (str "message received: " message))
            (cond
             (= msg "bid") (>! channel (str "thanks for " (if (= val "pass") "passing" "bidding")))
             (= msg "play") (>! channel (str "thanks for playing " val))
             (= msg "chat") (>! channel (chat val val2))
             :else (>! channel "unknown message type"))
            (recur))
          (println (str "channel closed")))))))

(defroutes app-routes
  (GET "/" [] (page-frame))
  (GET "/login" [] (page-login))
  (GET "/logout" [] (friend/logout* (resp/redirect "/"))))

(defroutes logged-in-routes
  (GET "/socky" []
       (friend/authenticated websocket-handler))
  (GET "/test-auth" []
       (friend/authenticated (page-dummy))))

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

(defonce server (atom nil))
(defn stop-server []
  (@server :timeout 100)
  (reset! server nil))
(defn -main []
  (reset! server (httpkit/run-server app {:port 8080})))
