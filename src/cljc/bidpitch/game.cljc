(ns bidpitch.game
  (:require [bidpitch.cards :refer [create-deck get-suit get-rank make-card ranks suits]])
  (#?(:clj :require :cljs :require-macros) [pallet.thread-expr :refer [arg-> if-> when-> when-not->]]))

;; Is a new hand automatically dealt? Yes, (true), except during tests
;; when we want to inspect the old state before it's replaced.
(def ^:dynamic *reconcile-end-game* true)
;; Are table cards cleared when last card is played or is there a
;; delay? Clear immediately during tests (true) but leave a delay
;; during normal game play (false) so players have a chance to see.
(def ^:dynamic *reconcile-hand-over* true)

; The maximum number of players in a game. The theoretical max is 8,
; since 8 x 6 = 48 cards. Some sources say 7. We may limit it to 4
; to ease development.
(def MAX_PLAYERS 8)

;; The number of points in a game.
(def MAX_POINTS 11)

;; Wanted to define `can-leave?` below `remove-player` to keep related
;; functions grouped together. Do the same for `can-join?` and
;; `add-players`.
(declare can-join?)
(declare can-leave?)

;; The base state of the game. All future states are transformations from here.
(def empty-state
  {:bids {}
   :dealer nil
   :messages []
   :onus nil
   :players []
   :player-cards {}
   :points {}
   :table-cards []
   :trump nil
   :winner nil})

; Helper functions for dealing cards
(defn deal-cards [deck num-players]
  (take num-players (partition 6 deck)))

; Getters from state
(defn get-players [state]
  (:players state))
(defn get-players-with-nils [state]
  (let [players (get-players state)
        non-empty (if (empty? players) [nil] players)
        n MAX_PLAYERS]
    (first (partition n n (repeat nil) non-empty))))
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
(defn get-onus [state]
  (:onus state))
(defn get-winner [state]
  (:winner state))
(defn get-messages [state]
  (:messages state))

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
  (let [onus (get-onus state)
        players (get-players state)]
    (assoc state :players (order-players players onus))))

; Other player functions
(defn leader [state]
  (first (get-players state)))
(defn leader? [state player]
  (= player (leader state)))
(defn has-player? [state player]
  (some #(= player %) (get-players state)))
(defn add-players [state players]
  (let [dedupe (filter (partial can-join? state) players)]
    (update-in state [:players] concat dedupe)))
(defn add-player [state & players]
  (add-players state players))
(defn remove-player [state player]
  (when (can-leave? state player)
    (update-in state [:players] #(remove #{player} %))))
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
(defn shield
  ([state user]
   (shield state user false))
  ([state user see-all-cards]
   (-> state
       (when-not-> see-all-cards
                   (assoc :player-cards (select-keys (:player-cards state) [user])))
       (assoc :me user))))

(defn clear-bids [state]
  (assoc state :bids {}))
(defn clear-trump [state]
  (assoc state :trump nil))
(defn clear-points [state]
  (-> state
      (assoc :points {})
      (assoc :winner nil)))
(defn clear-messages [state]
  (assoc state :messages []))
(defn starting-stage? [state]
  (nil? (get-dealer state)))
(def game-started? (comp not starting-stage?))
(defn can-start? [state player]
  (and (starting-stage? state)
       (leader? state player)
       (> (count (get-players state)) 1)))
(defn can-join? [state player]
  (and (not (game-started? state))
       (not (has-player? state player))
       (< (count (get-players state)) MAX_PLAYERS)))
(defn can-leave? [state player]
  (and (not (game-started? state))
       (has-player? state player)))
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
  (let [dealer (get-dealer state)]
    (-> state
        clear-points
        clear-messages
        add-cards
        (if-> dealer
              (dealt-state (next-player (get-players state) dealer))
              dealt-state))))

;; helper functions for managing bids
(defn bidding-stage? [state]
  (and (game-started? state)
       (< (count (get-bids state))
          (count (get-players state)))))
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
(defn everyone-played? [state]
  (let [num-players (count (get-players state))]
    (and (> num-players 0)
         (= num-players (count (get-table-cards state))))))
(defn who-won-hand [state]
  (let [lead-suit (get-lead-suit state)
        table-cards (get-table-cards state)
        trump (get-trump state)
        players (get-players state)
        highest-trump (highest table-cards trump)
        highest-lead-suit (highest table-cards lead-suit)
        winning-card (or highest-trump highest-lead-suit)]
    (when (everyone-played? state)
      (nth players (index-of table-cards winning-card)))))
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
(defn calc-points [state]
  (let [high-card (who-won-card state (get-highest-trump state))
        low-card (who-won-card state (get-lowest-trump state))
        jack-card (who-won-card state (make-card "J" (get-trump state)))
        most-points (most-game-pts state)
        one-or-inc #(if (nil? %) 1 (inc %))
        bidder (highest-bidder state)
        winning-bid (max-bid state)
        no-jack (-> (zipmap (get-players state) (repeat 0))
                    (update-in [high-card] one-or-inc)
                    (update-in [low-card] one-or-inc)
                    (update-in [jack-card] one-or-inc)
                    (update-in [most-points] one-or-inc))
        pts (dissoc no-jack nil)]
    (if (< (get pts bidder) winning-bid)
      (assoc pts bidder (- 0 winning-bid))
      pts)))
(defn round-over? [state]
  (empty? (get-all-cards state)))
(defn game-over? [state]
  (let [points (:points state)
        winning-pts (filter #(>= % MAX_POINTS) (vals points))]
    (and (not (empty? winning-pts))
         (= (count winning-pts) (count (into #{} winning-pts))))))
(defn needs-reconcile? [state]
  (and (not *reconcile-hand-over*) (everyone-played? state)))

;; state modifying functions for play action
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
(defn advance-onus [state]
  (assoc state :onus (when-not (everyone-played? state)
                       (next-player (get-players state) (get-onus state)))))
(defn award-hand-to-winner [state]
  (let [winner (who-won-hand state)]
    (-> state
        (update-in [:player-cards winner :tricks] conj (get-table-cards state))
        (assoc :onus winner)
        (order-around-onus))))
(defn check-hand-over [state]
  (if (everyone-played? state)
    (-> state award-hand-to-winner clear-table-cards)
    state))
(defn add-scores [state]
  (update-in state [:points] #(merge-with + % (calc-points state))))
(defn declare-winner [state]
  (if (game-over? state)
    (assoc state :winner (key (apply max-key val (:points state))))
    state))
(defn check-round-over [state]
  (if (round-over? state)
    (let [players (get-players state)
          dealer (get-dealer state)]
      (-> state
          add-scores
          (arg-> [new-state]
                 (if-> (game-over? new-state)
                       declare-winner
                       (when-> *reconcile-end-game*
                               add-cards
                               (dealt-state (next-player players dealer)))))))
      state))
(defn do-reconcile [state]
  (-> state
      (check-hand-over)
      (check-round-over)))

(defn update-play [old-state player value]
  (when (valid-play? old-state player value)
    (-> old-state
        (remove-card player value)
        (add-table-card value)
        (trump-if-none (get-suit value))
        (advance-onus)
        (if-> *reconcile-hand-over* do-reconcile))))

(defn advance-state [old-state player action value]
  (when-let [s (when (= (get-onus old-state) player)
                 (if (bidding-stage? old-state)
                   (when (= action "bid")
                     (update-bid old-state player value))
                   (when (= action "play")
                     (update-play old-state player value))))]
    (let [msg (str player " " action " " value)]
      (update-in s [:messages] #(conj % msg)))))

(defn bid [state player value]
  (advance-state state player "bid" value))
(defn play [state player value]
  (advance-state state player "play" value))
