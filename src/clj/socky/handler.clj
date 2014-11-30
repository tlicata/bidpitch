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
            [org.httpkit.server :as httpkit]
            [ring.util.response :as resp]
            [socky.db :as db]
            [socky.game :as game]
            [socky.view :as view]))

(def sockets (atom {}))
(def game-state (atom game/empty-state))

(defn update-clients! []
  (doseq [[user data] @sockets]
    (put! (:socket data) (prn-str (game/shield @game-state user)))))
(add-watch game-state nil (fn [key ref old-state new-state]
                            (update-clients!)))

(defn update-game! [func & vals]
  (when-let [new-state (apply func (concat [@game-state] vals))]
    (reset! game-state new-state)))

(defn convert-bid-to-int [str]
  (try
    (Integer/parseInt str)
    (catch NumberFormatException e -1)))

(defn player-join! [name]
  (update-game! game/add-player name))
(defn player-bid! [name value]
  (update-game! game/bid name (convert-bid-to-int value)))
(defn player-play! [name value]
  (update-game! game/play name value))
(defn player-start! []
  (update-game! game/restart))

(defn websocket-handler [request game-id]
  (with-channel request channel
    (when-let [username (:username (friend/current-authentication))]
      (swap! sockets assoc-in [game-id username] {:socket channel})
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
          (do
            (swap! sockets update-in [game-id] dissoc username)
            (println (str "channel closed"))))))))

(defroutes app-routes
  (GET "/" [] (view/page-home))
  (GET "/login" [] (view/page-login))
  (GET "/logout" [] (friend/logout* (resp/redirect "/"))))

(defroutes logged-in-routes
  (GET "/games/new" []
       (view/page-game-create))
  (GET "/games/:id" [id]
       (friend/authenticated (view/page-game id)))
  (POST "/games/" [title]
        (db/game-add title)
        (resp/redirect "/"))
  (GET "/games/" []
       (view/page-game-join (db/game-all)))
  (GET "/games/:id/socky" [id :as request]
       (friend/authenticated (websocket-handler request id))))

(defroutes fall-through-routes
  (route/resources "/")
  (route/not-found "Not Found"))

(defroutes all-routes
  app-routes
  (-> logged-in-routes
      (friend/authenticate {:credential-fn (partial creds/bcrypt-credential-fn db/player-get)
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
