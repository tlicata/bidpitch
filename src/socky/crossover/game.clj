(ns socky.crossover.game
  (:require [socky.crossover.cards :refer [create-deck suits]]
            [socky.handler :refer [users]]))

(def player {:id nil :name ""})
(def player-state {:cards [] :tricks []})

(def round {:dealer :id
            :player-states {}
            :bids {}
            :trump ""})

(def table {:players [] :scores []})

(def state {:table table :round round})

(defn deal-cards [deck num-players]
  (take num-players (partition 6 deck)))

(defn deal [deck players]
  (let [hands (deal-cards deck (count players))]
    (map (fn [hand player]
           {:id player
            :cards hand
            :tricks []})
         hands players)))

; Utility functions to get next player
(defn player-index [players player]
  (.indexOf players player))
(defn adjusted-index [players index]
  (let [num-players (count players)]
    (if (>= index num-players) 0 index)))
(defn next-index [players old-player]
  (let [index (inc (player-index players old-player))]
    (adjusted-index players index)))
(defn next-player [players old-player]
    (nth players (next-index players old-player)))

(def test-players (map #(:username (second %)) users))

(def test-table {:players test-players :scores [3 5]})
(defn test-round []
  {:dealer (rand-nth test-players)
   :player-states (deal (create-deck) test-players)
   :bids {}
   :trump nil})
