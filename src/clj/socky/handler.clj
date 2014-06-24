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
            [hiccup.page :refer [html5 include-css include-js]]
            [hiccup.element :refer [javascript-tag link-to]]
            [org.httpkit.server :as httpkit]
            [ring.util.response :as resp]
            [socky.game :as game]
            [socky.users :refer [users]]))

(defn button [link text]
  [:a.button {:href link} text])

(defn page-home []
  (html5
   [:head
    [:title "Bid Pitch - Home"]
    (include-css "/css/styles.css")]
   [:body.page.home
    [:div.row1
     [:h1 "Bid Pitch"]]
    [:div.row2
     [:div (button "/game-create" "Start Game")]
     [:div (button "/game-join" "Join Game")]]
    [:div.row3
     [:a.howto {:href ""} "How to play"]]]))

(defn page-game []
  (html5
   [:head
    [:title "Bid Pitch"]
    (include-css "/css/styles.css")
    (include-js "/js/lib/react-0.8.0.js" "/js/bin/main.js")]
   [:body [:div#content]]))

(defn page-game-create []
  (html5
   [:head
    [:title "Bid Pitch - Create Game"]
    (include-css "/css/styles.css")]
   [:body
    [:p "Make a game, fool"]]))

(defn page-game-join []
  (html5
   [:head
    [:title "Bid Pitch - Join Game"]
    (include-css "/css/styles.css")]
   [:body
    [:p "Join a game, fool"]]))

(defn page-login []
  (html5
   [:head
    [:title "Login"]]
   [:body
    [:form {:method "POST" :action "login"}
     [:div "Username" [:input {:type "text" :name "username"}]]
     [:div "Password" [:input {:type "password" :name "password"}]]
     [:div [:input {:type "submit" :class "button" :value "Login"}]]]]))

(def sockets (atom {}))
(def game-state (atom game/empty-state))

(defn update-clients! []
  (doseq [[user data] @sockets]
    (put! (:socket data) (prn-str (game/shield @game-state user)))))
(add-watch game-state nil (fn [key ref old-state new-state]
                            (update-clients!)))

(defn update-game-state! [func & vals]
  (when-let [new-state (apply func (concat [@game-state] vals))]
    (reset! game-state new-state)))

(defn convert-bid-to-int [str]
  (try
    (Integer/parseInt str)
    (catch NumberFormatException e -1)))

(defn player-join! [name]
  (update-game-state! game/add-player name))
(defn player-bid! [name value]
  (update-game-state! game/bid name (convert-bid-to-int value)))
(defn player-play! [name value]
  (update-game-state! game/play name value))
(defn player-start! []
  (update-game-state! game/restart))

(defn websocket-handler [request]
  (with-channel request channel
    (when-let [username (:username (friend/current-authentication))]
      (swap! sockets assoc username {:socket channel})
      (go-loop []
        (if-let [{:keys [message]} (<! channel)]
          (let [[msg val val2] (split message #":")]
            (println (str "message received: " message "  " username))
            (condp = msg
             "join" (player-join! username)
             "bid" (player-bid! username val)
             "play" (player-play! username val)
             "start" (player-start!)
             "state" (>! channel (prn-str (game/shield @game-state username)))
             :else (>! channel "unknown message type"))
            (recur))
          (println (str "channel closed")))))))

(defroutes app-routes
  (GET "/" [] (page-home))
  (GET "/login" [] (page-login))
  (GET "/logout" [] (friend/logout* (resp/redirect "/"))))

(defroutes logged-in-routes
  (GET "/game" []
       (friend/authenticated (page-game)))
  (GET "/game-create" []
       (friend/authenticated (page-game-create)))
  (GET "/game-join" []
       (friend/authenticated (page-game-join)))
  (GET "/socky" []
       (friend/authenticated websocket-handler)))

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
