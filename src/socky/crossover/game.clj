(ns socky.crossover.game
  (:require [socky.crossover.cards :refer [create-deck get-suit suits]]
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
(defn order-players [players dealer]
  (let [is-not-dealer (fn [player] (not= dealer player))
        before-dealer (take-while is-not-dealer players)
        dealer-and-after (drop-while is-not-dealer players)]
    (concat (rest dealer-and-after) before-dealer [dealer])))

(def test-players (map #(:username (second %)) users))

(def test-table {:players test-players :scores [3 5]})

(defn create-initial-state [players dealer]
  (let [ordered (order-players players dealer)]
    {:dealer dealer
     :player-states (deal (create-deck) ordered)
     :bids []
     :table-cards []
     :onus (next-player ordered dealer)
     :trump nil}))


(defn test-round []
  (let [dealer (rand-nth test-players)]
    (create-initial-state test-players dealer)))

;; helper functions for managing bids
(defn max-bid [bids]
  (if (= (count bids) 0) 0 (apply max bids)))
(defn highest-bidder [state]
  (let [bids (:bids state)
        players (map :id (:player-states state))
        highest (.indexOf bids (max-bid bids))]
    (when (not= highest -1)
      (nth players highest))))
(defn update-bid [old-state bids players player value]
  (if (and (= (count bids) (player-index players player))
           (or (= value 0)
               (and (> value (max-bid bids))
                    (> value 1)
                    (< value 5))))
    (let [new-state (assoc old-state :bids (conj bids value))]
      (if (= (count players) (count (:bids new-state)))
        (assoc new-state :onus (highest-bidder new-state))
        (assoc new-state :onus (next-player players player))))))

;; helper functions for managing cards
(defn cards-for-player [state player]
  (let [player-states (:player-states state)
        player-state (first (filter #(= player (:id %)) player-states))]
    (:cards player-state)))
(defn cards-by-suit [state player suit]
  (let [cards (cards-for-player state player)]
    (filter #(= suit (get-suit %)) cards)))
(defn player-has-card? [state player card]
  (some #(= card %) (cards-for-player state player)))
(defn valid-play? [old-state player value]
  (let [table-cards (:table-cards old-state)
        lead-suit (get-suit (first table-cards))
        suit (get-suit value)]
    (and (player-has-card? old-state player value)
         (or (empty? table-cards) ;; lead card is always valid
             (= suit lead-suit) ;; following suit is always valid
             (= suit (:trump old-state)) ;; trump is always valid
             (empty? (cards-by-suit old-state player lead-suit))))))

(defn update-play [old-state player value]
  (let [trump (:trump old-state)
        suit (get-suit value)]
    (when (valid-play? old-state player value)
      ;; (-> old-state
      ;;     (remove-card player value)
      ;;     (add-table-card value)
      ;;     (check-trump value)))))
      nil)))

(defn advance-state [old-state player action value]
  (let [bids (:bids old-state)
        onus (:onus old-state)
        players (map :id (:player-states old-state))
        bidding-complete (= (count bids) (count players))]
    (if (= onus player)
      (if bidding-complete
        (if (= action "play")
          (assoc old-state :onus (next-player players onus)))
        (if (= action "bid")
           (update-bid old-state bids players player value))))))
