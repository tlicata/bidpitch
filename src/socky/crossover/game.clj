(ns socky.crossover.game
  (:require [socky.crossover.cards :refer [create-deck]]))

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
           {:id (:id player)
            :cards hand
            :tricks []})
         hands players)))

(def tim {:id 1 :name "Tim"})
(def sharon {:id 2 :name "Sharon"})
(def louise {:id 3 :name "Louise"})
(def paul {:id 4 :name "Paul"})
(def test-players [tim sharon louise paul])

(def test-table {:players test-players :scores [3 5]})
(def test-round {:dealer tim
                 :player-states (deal (create-deck) test-players)
                 :bids {}
                 :trump ""})
