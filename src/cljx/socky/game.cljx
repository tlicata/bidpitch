(ns socky.game
  (:require [socky.cards :refer [create-deck get-suit get-rank make-card ranks suits]])
  (#+clj :require #+cljs :require-macros  [pallet.thread-expr :refer [arg-> if-> when->]]))

; Determines whether or not advance-state will infer additional
; state changes based on the initial change. For instance, if all
; the cards are played, a new hand should be started. Binding this
; value to false will prevent the new hand from being automatically
; dealt. This is only used for testing purposes.
(def ^:dynamic *reconcile* true)

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
(defn get-table-cards [state]
  (:table-cards state))
(defn get-trump [state]
  (:trump state))
(defn get-lead-suit [state]
  (get-suit (first (get-table-cards state))))
(defn get-dealer [state]
  (:dealer state))

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
(defn order-players [players start]
  (let [is-not-start #(not= start %)
        before-start (take-while is-not-start players)
        start-and-after (drop-while is-not-start players)]
    (concat start-and-after before-start)))
(defn order-around-onus [state]
  (let [onus (:onus state)
        players (get-players state)]
    (assoc state :players (order-players players onus))))

(def empty-state
  {:bids {}
   :dealer nil
   :onus nil
   :players []
   :player-cards {}
   :points {}
   :table-cards []
   :trump nil
   :winner nil})

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
(defn shield [state user]
  (-> state
      (assoc :player-cards (select-keys (:player-cards state) [user]))
      (assoc :me user)))

(defn clear-bids [state]
  (assoc state :bids {}))
(defn clear-trump [state]
  (assoc state :trump nil))
(defn clear-points [state]
  (-> state
      (assoc :points {})
      (assoc :winner nil)))
(defn game-started? [state]
  (not (nil? (get-dealer state))))
(defn can-join? [state player]
  (and (not (game-started? state))
       (not (has-player? state player))
       (< (count (get-players state)) 8)))
(defn dealt-state
  ([state]
     (dealt-state state (first (get-players state))))
  ([state dealer]
     (let [players (get-players state)
           onus (next-player players dealer)
           ordered (order-players players onus)]
       (-> state
           clear-bids
           clear-trump
           (assoc :dealer dealer)
           (assoc :onus onus)
           (assoc :players ordered)))))
(defn restart [state]
  (-> state clear-points add-cards dealt-state))

;; helper functions for managing bids
(defn bidding-stage? [state]
  (< (count (get-bids state)) (count (get-players state))))
(defn max-bid-entry [state]
  (let [bids (get-bids state)]
    (if (empty? bids)
      (first {nil 0})
      (apply max-key val bids))))
(defn max-bid [state]
  (val (max-bid-entry state)))
(defn highest-bidder [state]
  (key (max-bid-entry state)))
(defn valid-bid? [state player value]
  (let [leading (max-bid state)
        in-range (and (> value 1) (< value 5))]
    (if (= (count (get-bids state)) (player-index (get-players state) player))
      (if (and (= player (get-dealer state)) (= leading 0))
        in-range        ;; stick the dealer if no bids
        (or (= value 0) ;; else bid must be in range and above max (or pass)
            (and (> value leading) in-range))))))
(defn update-bid [old-state player value]
  (let [players (get-players old-state)]
    (if (valid-bid? old-state player value)
      (let [new-state (assoc-in old-state [:bids player] value)]
        (if (= (count players) (count (get-bids new-state)))
          (-> new-state
              (assoc :onus (highest-bidder new-state))
              (order-around-onus))
          (assoc new-state :onus (next-player players player)))))))

;; helper functions for managing cards
(defn cards-by-suit [state player suit]
  (let [cards (get-player-cards state player)]
    (filter #(= suit (get-suit %)) cards)))
(defn player-has-card? [state player card]
  (some #(= card %) (get-player-cards state player)))
(defn valid-play? [old-state player value]
  (let [table-cards (get-table-cards old-state)
        lead-suit (get-lead-suit old-state)
        suit (get-suit value)]
    (and (player-has-card? old-state player value)
         (or (empty? table-cards) ;; lead card is always valid
             (= suit lead-suit) ;; following suit is always valid
             (= suit (get-trump old-state)) ;; trump is always valid
             (empty? (cards-by-suit old-state player lead-suit))))))
(defn remove-card [state player card]
  (update-in state [:player-cards player :cards] #(remove #{card} %)))
(defn add-table-card [state card]
  (assoc state :table-cards (conj (:table-cards state) card)))
(defn clear-table-cards [state]
  (assoc state :table-cards []))
(defn trump-if-none [state suit]
  (if (nil? (get-trump state))
    (assoc state :trump suit)
    state))
(defn highest [cards suit]
  (let [matching (filter #(= suit (get-suit %)) cards)
        indices (map #(index-of ranks (get-rank %)) matching)]
    (when-not (empty? indices)
      (make-card (nth ranks (apply max indices)) suit))))
(defn lowest [cards suit]
  (let [matching (filter #(= suit (get-suit %)) cards)
        indices (map #(index-of ranks (get-rank %)) matching)]
    (when-not (empty? indices)
      (make-card (nth ranks (apply min indices)) suit))))
(defn determine-winner [state]
  (let [lead-suit (get-lead-suit state)
        table-cards (get-table-cards state)
        trump (get-trump state)
        players (get-players state)
        highest-trump (highest table-cards trump)
        highest-lead-suit (highest table-cards lead-suit)
        winning-card (or highest-trump highest-lead-suit)]
    (nth players (index-of table-cards winning-card))))
(defn award-trick-to-winner [state]
  (let [winner (determine-winner state)]
    (-> state
        (update-in [:player-cards winner :tricks] conj (get-table-cards state))
        (assoc :onus winner)
        (order-around-onus))))
(defn check-hand-winner [state player]
  (let [lead-suit (get-lead-suit state)
        players (get-players state)
        table-cards (get-table-cards state)
        trump (get-trump state)]
    (if (= (count table-cards) (count players))
      (-> state
          (award-trick-to-winner)
          (clear-table-cards))
      (assoc state :onus (next-player players player)))))
;; helper function for scoring tricks
(defn tally-game-pts [state player]
  (let [tricks (get-player-tricks state player)
        values {"A" 4, "K" 3, "Q" 2, "J" 1, "T" 10}
        get-value (fn [sum card]
                    (+ sum (get values (get-rank card) 0)))]
    (reduce get-value 0 (flatten tricks))))
(defn most-game-pts [state]
  (let [players (get-players state)
        pts (map (partial tally-game-pts state) players)]
    (when-not (empty? pts)
      (let [most (apply max pts)]
        (when (= (count (filter #{most} pts)) 1)
          (nth players (index-of pts most)))))))
(defn get-all-cards [state]
  (mapcat #(flatten (get-player-cards state %)) (get-players state)))
(defn get-all-tricks [state]
  (mapcat #(flatten (get-player-tricks state %)) (get-players state)))
(defn get-highest-trump [state]
  (highest (get-all-tricks state) (get-trump state)))
(defn get-lowest-trump [state]
  (lowest (get-all-tricks state) (get-trump state)))
(defn who-won-card [state card]
  (let [players (get-players state)]
    (first (drop-while #(nil? (index-of (flatten (get-player-tricks state %)) card)) players))))
(defn add-scores [state scores]
  (let [no-jack (dissoc scores nil)]
    (update-in state [:points] #(merge-with + % no-jack))))
(defn calc-points [state]
  (let [high-card (who-won-card state (get-highest-trump state))
        low-card (who-won-card state (get-lowest-trump state))
        jack-card (who-won-card state (make-card "J" (get-trump state)))
        most-points (most-game-pts state)
        one-or-inc #(if (nil? %) 1 (inc %))
        bidder (highest-bidder state)
        winning-bid (max-bid state)
        pts (-> (zipmap (get-players state) (repeat 0))
                (update-in [high-card] one-or-inc)
                (update-in [low-card] one-or-inc)
                (update-in [jack-card] one-or-inc)
                (update-in [most-points] one-or-inc))]
    (if (< (get pts bidder) winning-bid)
      (add-scores state (assoc pts bidder (- 0 winning-bid)))
      (add-scores state pts))))
(defn round-over? [state]
  (empty? (get-all-cards state)))
(defn game-over? [state]
  (let [points (:points state)
        winning-pts (filter #(>= % 11) (vals points))]
    (and (not (empty? winning-pts))
         (= (count winning-pts) (count (into #{} winning-pts))))))
(defn declare-winner [state]
  (if (game-over? state)
    (assoc state :winner (key (apply max-key val (:points state))))
    state))
(defn check-round-over [state]
  (if (round-over? state)
    (let [players (get-players state)
          dealer (get-dealer state)]
      (-> state
          calc-points
          (arg-> [new-state]
                 (if-> (game-over? new-state)
                       declare-winner
                       (when-> *reconcile*
                               add-cards
                               (dealt-state (next-player players dealer)))))))
      state))

(defn update-play [old-state player value]
  (let [trump (get-trump old-state)
        suit (get-suit value)]
    (when (valid-play? old-state player value)
      (-> old-state
          (remove-card player value)
          (add-table-card value)
          (trump-if-none (get-suit value))
          (check-hand-winner player)
          (check-round-over)))))

(defn advance-state [old-state player action value]
  (when (= (:onus old-state) player)
    (if (bidding-stage? old-state)
      (when (= action "bid")
        (update-bid old-state player value))
      (when (= action "play")
        (update-play old-state player value)))))

(defn bid [state player value]
  (advance-state state player "bid" value))
(defn play [state player value]
  (advance-state state player "play" value))
