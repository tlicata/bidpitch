(ns socky.ai
  (:require [clojure.core.async :refer [<!! >!! chan]]
            [socky.cards :as cards]
            [socky.game :as game]
            [socky.shield :as shield]))

(defn possible-bids [state]
  (let [player (game/get-onus state)
        valid? (partial game/valid-bid? state player)
        options (filter valid? [4 3 2 1 0])]
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
(defn possible-state [state {:keys [:action :value]}]
  ;; AI never needs to deal new cards in order to determine
  ;; play, since that is a new round.
  (binding [game/*reconcile-end-game* false]
    (game/advance-state state (game/get-onus state) action value)))

;;; Helper functions that mimic some functions in game.cljx but are
;;; custom to the AI behavior since they examine all the cards in the
;;; round, not just card that have been won already.
(defn won-or-lost-high [player state]
  {:pre [(empty? (game/get-table-cards state))]}
  (let [trump (game/get-trump state)
        played (game/highest (game/get-all-tricks state) trump)
        in-hand (game/highest (game/get-all-cards state) trump)
        highest (game/highest [played in-hand] trump)]
    (if (= played highest)
      (if (= (game/who-won-card state played) player) 1 -1)
      (if (game/player-has-card? state player in-hand) 1 -1))))
(defn won-or-lost-low [player state]
  (let [trump (game/get-trump state)
        played (game/lowest (game/get-all-tricks state) trump)
        in-hand (game/lowest (game/get-all-cards state) trump)
        lowest (game/lowest [played in-hand] trump)]
    (if (= played lowest)
      (if (= (game/who-won-card state lowest) player) 1 -1)
      0)))
(defn won-or-lost-jack [player state]
  (let [trump (game/get-trump state)
        jack (cards/make-card "J" trump)
        who (game/who-won-card state jack)]
    (condp = who player 1 nil 0 -1)))
(defn won-or-lost-pts [player state] 0)

(declare best-move)

;;; A static evaluation function that allows us to determine how
;;; promising a state is without playing it out to the bitter end.
(defn static-score [player state]
  (if (or (nil? (game/get-trump state))
          (not (empty? (game/get-table-cards state))))
    0
    (+ (won-or-lost-high player state) (won-or-lost-low player state)
       (won-or-lost-jack player state) (won-or-lost-pts player state))))

(defn best-move [state]
  (let [player (game/get-onus state)]
    (apply max-key #(static-score player (possible-state state %))
           (possible-moves state))))

(defn play [in out]
  (>!! in {:message "AI"})
  (let [jwt (<!! out) game-state (<!! out)]
    (>!! in {:message "join"})
    (loop []
      (when-let [game-state (read-string (<!! out))]
        (when (shield/my-turn? game-state)
          (Thread/sleep 2000)
          (let [{:keys [:action :value]} (best-move game-state)]
            (>!! in {:message (str action ":" value)})))
        (recur)))
    (println "AI stopped due to disconnect")))
