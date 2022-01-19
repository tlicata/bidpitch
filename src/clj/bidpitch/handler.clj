(ns bidpitch.handler
  (:use compojure.core)
  (:require [chord.http-kit :refer [with-channel]]
            [clojure.core.async :refer [<! >! chan close! go go-loop put!]]
            [clojure.string :refer [split trim]]
            [clojure.tools.reader.edn :as edn]
            [clj-jwt.core :refer [jwt str->jwt to-str verify]]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [org.httpkit.server :as httpkit]
            [ring.util.response :as resp]
            [bidpitch.ai :as ai]
            [bidpitch.game :as game]
            [bidpitch.view :as view])
  (:gen-class))

(def sockets (atom {}))
(def games (atom {}))

;; A hack to determine local AI connections from live player
;; connections. Websocket connections will be of class similar to
;; chord.channels$bidi_ch$reify__8637 while AI connections are pure
;; core.async channels.
(defn local-ai? [socket]
  (instance? clojure.core.async.impl.channels.ManyToManyChannel socket))

(defn get-sockets [game-id]
  (get @sockets game-id))
(defn find-socket [game-id username socks]
  (-> socks (get game-id) (get username)))
(defn add-socket! [game-id username channel force]
  (= channel
     (find-socket
      game-id username
      (swap! sockets (fn [old-sockets]
                       (let [update #(assoc-in old-sockets [game-id username] channel)]
                         (if-let [sock (find-socket game-id username old-sockets)]
                           (if force (do (close! sock) (update)) old-sockets)
                           (update))))))))
(defn remove-socket! [game-id username socket]
  (swap! sockets (fn [old]
                   (if (= socket (find-socket game-id username old))
                     (update-in old [game-id] dissoc username)
                     old))))

(defn get-game! [game-id]
  (get @games game-id))
(defn add-game! [game-id]
  (when-not (get-game! game-id)
    (swap! games assoc game-id game/empty-state)))

(defn state-to-client [state user socket]
  (prn-str (game/shield state user (local-ai? socket))))
(defn update-clients! [game-id]
  (let [game-state (get-game! game-id)]
    (doseq [[user socket] (get-sockets game-id)]
      (put! socket (state-to-client game-state user socket)))))
(defn update-game! [game-id func & vals]
  (binding [game/*reconcile-hand-over* false]
    (when-let [new-state (apply func (concat [(get-game! game-id)] vals))]
      (swap! games assoc game-id new-state)
      (when (game/needs-reconcile? new-state)
        (future
          (Thread/sleep 2000)
          (update-game! game-id game/do-reconcile)))
      (update-clients! game-id))))

(defn write-games-to-disk! []
  (spit "games.edn" (prn-str @games)))
(defn read-games-from-disk! []
  (reset! games (edn/read-string (slurp "games.edn"))))

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

(declare register-channel)

(defn spawn-ai [game-id]
  (future (let [in (chan) out (chan)]
            (register-channel game-id in out)
            (ai/play in out))))

(defn grab-user-name [msg]
  (let [possible-jwt (try (str->jwt msg) (catch Exception _ (trim msg)))]
    (if (string? possible-jwt)
      {:signed false :jwt (jwt {:username possible-jwt}) :username possible-jwt}
      {:signed true :jwt possible-jwt :username (-> possible-jwt :claims :username)})))

(defn register-channel [game-id in out]
  (go
    (let [{:keys [username jwt signed]} (grab-user-name (:message (<! in)))]
      (if (add-socket! game-id username out signed)
        (do
          (>! out (prn-str (to-str jwt)))
          (>! out (state-to-client (get-game! game-id) username out))
          (loop []
            (if-let [{:keys [message]} (<! in)]
              (let [[msg val val2] (split message #":")]
                (println (str "message received: " username  " " message))
                (condp = msg
                  "ai" (spawn-ai game-id)
                  "join" (player-join! game-id username)
                  "leave" (player-leave! game-id username)
                  "bid" (player-bid! game-id username val)
                  "play" (player-play! game-id username val)
                  "start" (player-start! game-id)
                  (>! out (state-to-client (get-game! game-id) username out)))
                (recur))
              (do
                (player-leave! game-id username)
                (remove-socket! game-id username out)
                (println (str "channel closed by " username))))))
        (>! out "taken")))))

(defn websocket-handler [request game-id]
  (with-channel request channel
    (register-channel game-id channel channel)))

(defroutes app-routes
  (GET "/" request
       (let [jwt (get-in request [:cookies "username" :value])
             username (when jwt (:username (grab-user-name jwt)))]
         (view/page-home @games username)))
  (POST "/games/" []
        (let [id (str (java.util.UUID/randomUUID))]
          (add-game! id)
          (resp/redirect (str "/games/" id))))
  (GET "/games/:id" [id]
       (when (get-game! id)
         (view/page-game id)))
  (GET "/games/:id/socky" [id :as request]
       (websocket-handler request id))
  (GET "/rename" [] (view/page-rename))
  (route/resources "/")
  (route/not-found (view/page-not-found)))

(def app
  (handler/site app-routes))

(defonce server (atom nil))
(defn stop-server []
  (@server :timeout 100)
  (reset! server nil))
(defn -main []
  (let [port (Integer/parseInt (or (System/getenv "PORT") "8080"))]
    (reset! server (httpkit/run-server app {:port port}))))
