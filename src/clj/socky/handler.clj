(ns socky.handler
  (:use compojure.core)
  (:require [chord.http-kit :refer [with-channel]]
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
(def games (atom {}))

(defn get-sockets [game-id]
  (get @sockets game-id))
(defn add-socket! [game-id username channel]
  (when-not (get-in @sockets [game-id username])
    (swap! sockets assoc-in [game-id username] {:socket channel})))
(defn remove-socket! [game-id username]
  (swap! sockets update-in [game-id] dissoc username))

(defn get-game [game-id]
  (get @games game-id))
(defn add-game! [game-id]
  (when-not (get-game game-id)
    (swap! games assoc game-id game/empty-state)))

(defn update-clients! [game-id]
  (let [game-state (get-game game-id)]
    (doseq [[user data] (get-sockets game-id)]
      (put! (:socket data) (prn-str (game/shield game-state user))))))
(defn update-game! [game-id func & vals]
  (binding [game/*reconcile-hand-over* false]
    (when-let [new-state (apply func (concat [(get-game game-id)] vals))]
      (swap! games assoc game-id new-state)
      (when (game/needs-reconcile? new-state)
        (future
          (Thread/sleep 2000)
          (update-game! game-id game/do-reconcile)))
      (update-clients! game-id))))

(defn convert-str-to-int [str]
  (try
    (Integer/parseInt str)
    (catch NumberFormatException e -1)))

(defn player-join! [game-id name]
  (update-game! game-id game/add-player name))
(defn player-leave! [game-id name]
  (update-game! game-id game/remove-player name))
(defn player-bid! [game-id name value]
  (update-game! game-id game/bid name (convert-str-to-int value)))
(defn player-play! [game-id name value]
  (update-game! game-id game/play name value))
(defn player-start! [game-id]
  (update-game! game-id game/restart))

(defn websocket-handler [request game-id]
  (with-channel request channel
    (go
      (let [{username :message} (<! channel)]
        (add-game! game-id)
        (if (add-socket! game-id username channel)
          (do
            (println (str "channel for user: " username))
            (>! channel (prn-str (game/shield (get-game game-id) username)))
            (loop []
              (if-let [{:keys [message]} (<! channel)]
                (let [[msg val val2] (split message #":")]
                  (println (str "message received: " game-id " " message "  " username))
                  (condp = msg
                    "join" (player-join! game-id username)
                    "leave" (player-leave! game-id username)
                    "bid" (player-bid! game-id username val)
                    "play" (player-play! game-id username val)
                    "start" (player-start! game-id)
                    "state" (>! channel (prn-str (game/shield (get-game game-id) username)))
                    :else (>! channel "unknown message type"))
                  (recur))
                (do
                  (player-leave! game-id username)
                  (remove-socket! game-id username)
                  (println (str "channel closed by " username))))))
          (>! channel "taken"))))))

(defroutes app-routes
  (GET "/" [] (view/page-home @games))
  (POST "/games/" []
        (let [{:keys [id]} (db/game-add)]
          (resp/redirect (str "/games/" id))))
  (GET "/games/:id" [id]
       (when (db/game-get (convert-str-to-int id))
         (view/page-game id)))
  (GET "/games/:id/socky" [id :as request]
       (websocket-handler request id))
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (handler/site app-routes))

(defonce server (atom nil))
(defn stop-server []
  (@server :timeout 100)
  (reset! server nil))
(defn -main []
  (reset! server (httpkit/run-server app {:port 8080})))
