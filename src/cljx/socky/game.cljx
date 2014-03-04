(ns socky.game
  (:require [socky.cards :refer [create-deck get-suit suits]]))

; Helper functions for dealing cards
(defn deal-cards [deck num-players]
  (take num-players (partition 6 deck)))

; Getters from state
(defn get-players [state]
  (:players state))
(defn get-player-state [state player]
  (get (:player-cards state) player))
(defn get-player-cards [state player]
  (:cards (get-player-state state player)))
(defn get-player-tricks [state player]
  (:tricks (get-player-state state player)))
(defn get-bids [state]
  (:bids state))

; Utility functions to get next player
(defn index-of [vect item]
  (first (keep-indexed #(if (= %2 item) %1) vect)))
(defn player-index [players player]
  (index-of players player))
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

(def empty-state
  {:bids []
   :dealer nil
   :onus nil
   :players []
   :player-cards {}
   :table-cards []
   :trump nil})

(defn has-player? [state player]
  (some #{player} (get-players state)))
(defn add-players [state players]
  (let [dedupe (filter #(not (has-player? state %)) players)]
    (update-in state [:players] concat dedupe)))
(defn add-player [state & players]
  (add-players state players))
(defn add-cards
  ;; deal random cards
  ([state]
     (let [players (get-players state)
           cards (deal-cards (create-deck) (count players))
           both (zipmap players cards)]
       (reduce #(add-cards %1 (first %2) (second %2)) state both)))
  ;; give specific cards to player
  ([state player cards]
     (assoc-in state [:player-cards player] {:cards cards :tricks []})))

(defn dealt-state [state dealer]
  (let [players (get-players state)
        ordered (order-players players dealer)]
    (-> state
        (assoc :dealer dealer)
        (assoc :onus (next-player ordered dealer))
        (assoc :players ordered))))

;; helper functions for managing bids
(defn max-bid [bids]
  (if (= (count bids) 0) 0 (apply max bids)))
(defn highest-bidder [state]
  (let [bids (get-bids state)
        players (get-players state)
        highest (index-of bids (max-bid bids))]
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
(defn cards-by-suit [state player suit]
  (let [cards (get-player-cards state player)]
    (filter #(= suit (get-suit %)) cards)))
(defn player-has-card? [state player card]
  (some #(= card %) (get-player-cards state player)))
(defn valid-play? [old-state player value]
  (let [table-cards (:table-cards old-state)
        lead-suit (get-suit (first table-cards))
        suit (get-suit value)]
    (and (player-has-card? old-state player value)
         (or (empty? table-cards) ;; lead card is always valid
             (= suit lead-suit) ;; following suit is always valid
             (= suit (:trump old-state)) ;; trump is always valid
             (empty? (cards-by-suit old-state player lead-suit))))))
(defn remove-card [state player card]
  (update-in state [:player-cards player] #(remove #{card} %)))
(defn add-table-card [state card]
  (assoc state :table-cards (conj (:table-cards state) card)))
(defn check-trump [state suit]
  (if (nil? (:trump state))
    (assoc state :trump suit)
    state))

(defn update-play [old-state player value]
  (let [trump (:trump old-state)
        suit (get-suit value)]
    (when (valid-play? old-state player value)
      (-> old-state
          (remove-card player value)
          (add-table-card value)
          (check-trump (get-suit value))))))

(defn advance-state [old-state player action value]
  (let [bids (get-bids old-state)
        onus (:onus old-state)
        players (get-players old-state)
        bidding-complete (= (count bids) (count players))]
    (when (= onus player)
      (if bidding-complete
        (when (= action "play")
          (update-play old-state player value))
        (when (= action "bid")
           (update-bid old-state bids players player value))))))
