(ns bidpitch.ai
  (:require [clojure.core.async :refer [<!! >!! chan]]
            [clojure.core.memoize :as memoize]
            [bidpitch.cards :as cards]
            [bidpitch.game :as game]
            [bidpitch.shield :as shield])
  (:import (java.util.concurrent TimeoutException TimeUnit FutureTask)))

(defn possible-bids [state]
  (let [player (game/get-onus state)
        valid? (partial game/valid-bid? state player)
        options (filter valid? [0 2])]
    (map (fn [bid] {:action "bid" :value bid}) options)))
(defn possible-cards [state]
  (let [player (game/get-onus state)
        valid? (partial game/valid-play? state player)
        options (filter valid? (game/get-player-cards state player))]
    (map (fn [card] {:action "play" :value card}) options)))
(defn possible-moves [state]
  (cond
    (game/starting-stage? state) [{:action "start"}]
    (game/bidding-stage? state) (possible-bids state)
    :else (possible-cards state)))
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

(declare best-move-memo)

;;; A static evaluation function that allows us to determine how
;;; promising a state is without playing it out to the bitter end.
(defn static-score [player state]
  (if (or (nil? (game/get-trump state))
          (not (empty? (game/get-table-cards state))))
    0
    (+ (won-or-lost-high player state) (won-or-lost-low player state)
       (won-or-lost-jack player state) (won-or-lost-pts player state))))

;; Prune possible-moves based on the static evalutation function.
(defn prune [player state moves]
  (let [states (map (partial possible-state state) moves)
        scored (map #(assoc % :score (static-score player %)) states)
        zipped (zipmap scored moves)
        sorted (into (sorted-map-by (fn [x y] (>= (:score x) (:score y)))) zipped)
        get-score (comp :score key)
        best-score (get-score (first sorted))
        actions (vals (take-while #(= (get-score %) best-score) sorted))]
    (if (>= best-score 2)
      (take 1 actions)
      (take 2 actions))))

(defn expected-score [player state]
  (if (game/round-over? state)
    (let [points (game/calc-points state)
          our-points (get points player)
          their-points (vals (dissoc points player))
          avg-them (/ (reduce + their-points) (count their-points))]
      (- our-points avg-them))
    (recur player (possible-state state (best-move-memo state)))))

(defn best-move [state]
  (let [player (game/get-onus state)
        moves (possible-moves state)]
    (if (= 1 (count moves))
      (first moves)
      (apply max-key #(expected-score player (possible-state state %))
             (prune player state moves)))))

(def best-move-memo (memoize/ttl best-move {} :ttl/threshold (* 240000)))

;; Thread timeout helpers inspired by code in Clojail.
;; https://github.com/flatland/clojail/blob/master/src/clojail/core.clj
(defn thunk-timeout [thunk time]
  (let [task (FutureTask. thunk)
        thread (Thread. task)]
    (try
      (.start thread)
      (.get task time TimeUnit/MILLISECONDS)
      (catch TimeoutException e
        (.cancel task true)
        (.stop thread)
        (throw (TimeoutException. "Execution timed out.")))
      (catch Exception e
        (.cancel task true)
        (.stop thread)
        (throw e)))))
(defmacro with-timeout [time & body]
  `(thunk-timeout (fn [] ~@body) ~time))

(defn best-move-timeout [state]
  (try (with-timeout 10000 (best-move-memo state))
       (catch TimeoutException _
         (println "Timeout exception caught. AI picking randomly.")
         (first (possible-moves state)))))

(defn play [in out]
  (>!! in {:message "AI"})
  (let [jwt (<!! out) game-state (<!! out)]
    (>!! in {:message "join"})
    (loop []
      (when-let [game-state (read-string (<!! out))]
        (when (shield/my-turn? game-state)
          (Thread/sleep 1000)
          (let [{:keys [:action :value]} (best-move-timeout game-state)]
            (>!! in {:message (str action ":" value)})))
        (recur)))
    (println "AI stopped due to disconnect")))
