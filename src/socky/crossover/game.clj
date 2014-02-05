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

(def test-players (map #(:username (second %)) users))

(def test-table {:players test-players :scores [3 5]})
(defn test-round []
  {:dealer (rand-nth test-players)
   :player-states (deal (create-deck) test-players)
   :bids {}
   :trump (rand-nth suits)})
