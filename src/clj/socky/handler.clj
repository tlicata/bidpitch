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

(defn- include-cljs [path]
  (list
   (javascript-tag "var CLOSURE_NO_DEPS = true;")
   (include-js path)))

(defn page-home []
  (html5
   [:head
    [:title "Bid Pitch - Home"]
    (include-css "/css/styles.css")]
   [:body
    [:p "Welcome to Bid Pitch"]
    (link-to "/game-create" "create game")
    (link-to "/game-join" "join game")]))

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

(def sockets (atom {}))
(def game-state (atom game/empty-state))

(defn convert-bid-to-int [str]
  (try
    (Integer/parseInt str)
    (catch NumberFormatException e -1)))

(defn player-join [name]
  (when-let [new-state (game/add-player @game-state name)]
    (reset! game-state new-state)))

(defn player-bid [name value]
  (when-let [new-state (game/bid @game-state name (convert-bid-to-int value))]
    (reset! game-state new-state)))

(defn player-play [name value]
  (when-let [new-state (game/play @game-state name value)]
    (reset! game-state new-state)))

(defn player-start []
  (when-let [new-state (-> @game-state
                           game/add-cards
                           game/dealt-state)]
    (reset! game-state new-state)))

(defn update-all []
  (doseq [[user data] @sockets]
    (put! (:socket data) (prn-str (game/shield @game-state user)))))

(add-watch game-state nil (fn [key ref old-state new-state]
                            (update-all)))

(defn websocket-handler [request]
  (with-channel request channel
    (when-let [username (:username (friend/current-authentication))]
      (swap! sockets assoc username {:socket channel})
      (go-loop []
        (if-let [{:keys [message]} (<! channel)]
          (let [[msg val val2] (split message #":")]
            (println (str "message received: " message "  " username))
            (cond
             (= msg "join") (player-join username)
             (= msg "bid") (player-bid username val)
             (= msg "play") (player-play username val)
             (= msg "start") (player-start)
             (= msg "state") (>! channel (prn-str (game/shield @game-state username)))
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
