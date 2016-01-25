(ns socky.shield
  (:require [socky.game :as game]))

(defn who-am-i [state]
  (:me state))
(defn my-turn? [state]
  (= (game/get-onus state) (who-am-i state)))
(defn my-turn-to-bid? [state]
  (and (my-turn? state) (game/bidding-stage? state)))
(defn my-turn-to-play? [state]
  (and (my-turn? state) (not (game/bidding-stage? state))))
(defn my-cards [state]
  (when-let [us (first (:player-cards state))]
    (:cards (val us))))
