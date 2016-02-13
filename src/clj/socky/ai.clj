(ns socky.ai
  (:require [clojure.core.async :refer [<!! >!! chan]]
            [socky.game :as game]
            [socky.shield :as shield]))

(defn possible-bids [state]
  (let [player (game/get-onus state)
        valid? (partial game/valid-bid? state player)
        options (filter valid? [0 1 2 3 4])]
    (map (fn [bid] {:action "bid" :value bid}) options)))
(defn possible-cards [state]
  (let [player (game/get-onus state)
        valid? (partial game/valid-play? state player)
        options (filter valid? (game/get-player-cards state player))]
    (map (fn [card] {:action "play" :value card}) options)))
(defn possible-moves [state]
  (if (game/bidding-stage? state)
    (possible-bids state)
    (possible-cards state)))
(defn possible-next-states [state]
  (map (fn [{:keys [:action :value]}]
         ;; AI never needs to deal new cards in order to determine
         ;; play, since that is a new round.
         (binding [game/*reconcile-end-game* false]
           (game/advance-state state (game/get-onus state) action value)))
       (possible-moves state)))

(defn play [in out]
  (>!! in {:message "AI"})
  (let [jwt (<!! out) game-state (<!! out)]
    (>!! in {:message "join"})
    (loop []
      (when-let [game-state (read-string (<!! out))]
        (when (shield/my-turn? game-state)
          (Thread/sleep 2000)
          (>!! in {:message
                   (if (shield/my-turn-to-bid? game-state)
                     (str "bid:" (first (filter (partial game/valid-bid? game-state (shield/who-am-i game-state)) [0 1 2 3 4])))
                     (str "play:" (rand-nth (filter (partial game/valid-play? game-state (shield/who-am-i game-state)) (shield/my-cards game-state)))))}))
        (recur)))
    (println "AI stopped due to disconnect")))
