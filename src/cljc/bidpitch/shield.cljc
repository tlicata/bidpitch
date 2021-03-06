(ns bidpitch.shield
  (:require [bidpitch.game :as game]))

(defn who-am-i [state]
  (:me state))
(defn am-i-leader? [state]
  (game/leader? state (who-am-i state)))
(defn can-i-start? [state]
  (game/can-start? state (who-am-i state)))
(defn my-turn? [state]
  (if (game/starting-stage? state)
    (can-i-start? state)
    (and (not (game/game-over? state))
         (= (game/get-onus state) (who-am-i state)))))
(defn my-turn-to-start? [state]
  (and (my-turn? state) (game/starting-stage? state)))
(defn my-turn-to-bid? [state]
  (and (my-turn? state) (game/bidding-stage? state)))
(defn my-turn-to-play? [state]
  (and (my-turn? state) (not (or (game/bidding-stage? state)
                                 (game/starting-stage? state)))))
(defn my-cards [state]
  (game/get-player-cards state (who-am-i state)))
