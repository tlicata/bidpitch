(ns bidpitch.test.ai
  (:use clojure.test bidpitch.ai)
  (:require [clojure.core.async :refer [<!! >!! chan]]
            [clojure.set :refer [subset?]]
            [bidpitch.game :as game]))

(deftest test-possible-moves
  (let [my-name "AI" opponent "opponent-name"
        state (-> game/empty-state
                  (game/add-player my-name opponent)
                  (game/add-cards my-name ["AC" "KC" "JC"])
                  (game/add-cards opponent ["2D" "4D" "6D"])
                  (game/dealt-state))]
    (testing "possible bids"
      (is (= (possible-moves state) [{:action "bid" :value 0}
                                     {:action "bid" :value 2}]))
      (is (= (possible-moves (-> state (game/bid opponent 0)))
             [{:action "bid" :value 2}])))
    (testing "possible cards"
      (is (= (possible-moves (-> state (game/bid opponent 0)
                                 (game/bid my-name 2)))
             [{:action "play" :value "AC"}
              {:action "play" :value "KC"}
              {:action "play" :value "JC"}]))
      (is (= (possible-moves (-> state (game/bid opponent 2)
                                 (game/bid my-name 0)))
             [{:action "play" :value "2D"}
              {:action "play" :value "4D"}
              {:action "play" :value "6D"}])))))

(deftest test-possible-next-states
  (testing "possible next states"
    (let [my-name "AI" opponent "opponent-name"
          state (-> game/empty-state
                    (game/add-player my-name opponent)
                    (game/add-cards my-name ["AC" "KC" "JC"])
                    (game/add-cards opponent ["2D" "4D" "6D"])
                    (game/dealt-state)
                    (game/bid opponent 2) (game/bid my-name 0)
                    (game/play opponent "6D") (game/play my-name "JC")
                    (game/play opponent "4D") (game/play my-name "KC"))
          one-step (-> state (game/play opponent "2D"))]
      (is (= (possible-state state (first (possible-moves state))) one-step))
      (binding [game/*reconcile-end-game* false]
        (is (= (possible-state one-step (first (possible-moves one-step)))
               (-> one-step (game/play my-name "AC"))))))))

(deftest test-ai-helpers
  (testing "who has what cards"
    (let [my-name "AI" opponent "other"
          base (-> game/empty-state
                    (game/add-player my-name opponent)
                    (game/add-cards my-name ["AC" "KC" "JC"])
                    (game/add-cards opponent ["2D" "4D" "6D"])
                    (game/dealt-state))]

      ;; AI picks trump.
      (let [state (-> base (game/bid opponent 0) (game/bid my-name 2))]
        (let [high (-> state
                       (game/play my-name "AC") (game/play opponent "2D"))]
          (is (= 1 (won-or-lost-high my-name high)))
          (is (= 0 (won-or-lost-low my-name high)))
          (is (= 0 (won-or-lost-jack my-name high)))
          (is (= -1 (won-or-lost-high opponent high)))
          (is (= 0 (won-or-lost-low opponent high)))
          (is (= 0 (won-or-lost-jack opponent high))))
        (let [low (-> state
                      (game/play my-name "JC") (game/play opponent "2D"))]
          (is (= 1 (won-or-lost-high my-name low)))
          (is (= 1 (won-or-lost-low my-name low)))
          (is (= 1 (won-or-lost-jack my-name low)))
          (is (= -1 (won-or-lost-high opponent low)))
          (is (= -1 (won-or-lost-low opponent low)))
          (is (= -1 (won-or-lost-jack opponent low)))))

      ;; Opponent picks trump.
      (let [state (-> base (game/bid opponent 2) (game/bid my-name 0))]
        (let [high (-> state
                       (game/play opponent "4D") (game/play my-name "KC"))]
          (is (= -1 (won-or-lost-high my-name high)))
          (is (= 0 (won-or-lost-low my-name high)))
          (is (= 0 (won-or-lost-jack my-name high)))
          (is (= 1 (won-or-lost-high opponent high)))
          (is (= 0 (won-or-lost-low opponent high)))
          (is (= 0 (won-or-lost-jack opponent high))))
        (let [low (-> state
                      (game/play opponent "2D") (game/play my-name "JC"))]
          (is (= -1 (won-or-lost-high my-name low)))
          (is (= -1 (won-or-lost-low my-name low)))
          (is (= 0 (won-or-lost-jack my-name low)))
          (is (= 1 (won-or-lost-high opponent low)))
          (is (= 1 (won-or-lost-low opponent low)))
          (is (= 0 (won-or-lost-jack opponent low))))))))

(deftest test-static-score
  (testing "static evaluation of state"
    (let [my-name "AI" opponent "other"
          base (-> game/empty-state
                    (game/add-player my-name opponent)
                    (game/add-cards my-name ["AC" "KC" "JC"])
                    (game/add-cards opponent ["2D" "4D" "6D"])
                    (game/dealt-state))]
      ;; AI picks trump.
      (let [state (-> base (game/bid opponent 0) (game/bid my-name 2))]
        ;; 1 point for high card.
        (is (= 1 (static-score my-name (-> state (game/play my-name "AC")
                                           (game/play opponent "2D")))))
        ;; 3 points for high, low, and jack.
        (is (= 3 (static-score my-name (-> state (game/play my-name "JC")
                                           (game/play opponent "2D"))))))
      ;; Opponent picks trump.
      (let [state (-> base (game/bid opponent 2) (game/bid my-name 0))]
        ;; -2 since opponent has high (in hand) and low (played).
        (is (= -2 (static-score my-name (-> state (game/play opponent "2D")
                                            (game/play my-name "KC")))))
        ;; -1 since opponent has high (played).
        (is (= -1 (static-score my-name (-> state (game/play opponent "6D")
                                            (game/play my-name "JC")))))
        ;; -1 since opponent has high (in hand).
        (is (= -1 (static-score my-name (-> state (game/play opponent "4D")
                                            (game/play my-name "JC")))))))))

(deftest test-prune
  (testing "likely best moves without recursing"
    (let [my-name "AI" opponent "other"
          base (-> game/empty-state
                    (game/add-player my-name opponent)
                    (game/add-cards my-name ["AC" "TD" "4D"])
                    (game/add-cards opponent ["2C" "3C" "6D"])
                    (game/dealt-state))]
      (let [state (-> base (game/bid opponent 0) (game/bid my-name 2))]
        ;; All moves are equivalent when statically considered.
        (is (subset?
             (set (prune my-name state (possible-moves state)))
             (set [{:action "play" :value "4D"}
                   {:action "play" :value "TD"}
                   {:action "play" :value "AC"}])))
        (let [lead (-> state (game/play my-name "AC"))
              moves (possible-moves lead)]
          ;; Opponent needs to follow suit.
          (is (= moves [{:action "play" :value "2C"}
                        {:action "play" :value "3C"}]))
          ;; But it should not throw the low.
          (is (= (prune opponent lead moves)
                 [{:action "play" :value "3C"}]))
          (let [final (-> lead
                          (game/play opponent "3C")
                          (game/play my-name "TD"))]
            ;; Opponent should use the low to take the ten!
            (is (= (prune opponent final (possible-moves final))
                   [{:action "play" :value "2C"}]))))))))

(deftest test-expected-score
  (testing "expected score of a state for a player"
    (let [my-name "AI" opponent "opponent"
          base (-> game/empty-state
                    (game/add-player my-name opponent)
                    (game/add-cards my-name ["AC" "TD" "4D"])
                    (game/add-cards opponent ["2C" "3C" "6D"])
                    (game/dealt-state)
                    (game/bid opponent 0) (game/bid my-name 2)
                    (game/play my-name "AC"))]
      (binding [game/*reconcile-end-game* false]
        (let [state (-> base (game/play opponent "3C")
                        (game/play my-name "TD") (game/play opponent "2C")
                        (game/play opponent "6D"))
              done (-> state (game/play my-name "4D"))]
          ;; AI loses 2 (doesn't make bid) and opponent gets 2 (low + pts).
          (is (= (expected-score my-name done) -4))
          (is (= (expected-score opponent done) 4))
          ;; Can the computer figure out that out with one move left?
          (is (= (expected-score my-name state) -4))
          ;; But from the beginning the AI can play smarter.
          (is (= (expected-score my-name base) 1)))))))

;; Helper function for serializing state to be sent to AI.
(defn state-to-ai [state username]
  (prn-str (game/shield state username true)))

;; This function performs the initial handshake that the AI client
;; (indeed, all clients, including the browser) go through when
;; connecting to the server. It is used in the tests below.
(defn setup-game [my-name from-ai to-ai]

  ;; Expect to receive "name" message from AI.  Every client
  ;; tries to register with a name and is accepted if the name
  ;; is available and rejected otherwise.
  (is (= {:message my-name} (<!! from-ai)))

  ;; We'll accept the name "AI". In this case the server usually
  ;; sends back a signed JWT, giving this client the privilege to
  ;; that name in case of future collisions. Really, we can send
  ;; back anything other than "taken".
  (>!! to-ai "your-name-was-accepted")

  ;; After accepting the client, we send them the current state of
  ;; the game (shielded so they can't see other players' cards).
  (>!! to-ai (state-to-ai game/empty-state my-name))

  ;; Expect to receive the "join" message from the client saying
  ;; the AI wants to join the game.
  (is (= {:message "join"} (<!! from-ai)))

  ;; The client will now sit in a loop, waiting for updates from
  ;; the server. The browser will show this new game state to the
  ;; player and respond with player action. The AI will examine
  ;; the state and try to come up with its own response (if it is
  ;; its turn to play).
)

(deftest test-ai
  (testing "AI bidding behavior"
    (let [my-name "AI"
          from-ai (chan) to-ai (chan)
          brain (future (play from-ai to-ai))]

      (setup-game my-name from-ai to-ai)

      ;; Let's make up some states and see how the AI responds.
      (let [opponent "opponent-name"
            state (-> game/empty-state
                      ;; Add the AI and another player
                      (game/add-player my-name opponent)
                      ;; Give them each some cards.
                      (game/add-cards my-name ["AC" "KC" "JC"])
                      (game/add-cards opponent ["2D" "4D" "6D"])
                      ;; Set dealer and order players accordingly.
                      (game/dealt-state))]
        (is (= (game/get-dealer state) my-name))
        (is (= (game/get-onus state) opponent))

        ;; If opponent passes, then AI will bid 2.
        (>!! to-ai (-> state
                       (game/bid opponent 0)
                       (state-to-ai my-name)))
        (is (= {:message "bid:2"} (<!! from-ai))))

      ;; Try to clean up the running AI process.
      (future-cancel brain)))

  ;; There once existed a bug who only showed its face when:
  ;;  1) a game had ended and a new one started, and
  ;;  2) the AI was the second player to bid.
  ;; Let's try to recreate that scenario here.
  (testing "AI response to game restarts"
    (let [my-name "AI"
          opponent "opponent"
          points (assoc {} my-name (dec game/MAX_POINTS) opponent 0)
          from-ai (chan) to-ai (chan)
          brain (future (play from-ai to-ai))
          state (-> game/empty-state
                    (assoc :points points) ;; about to win
                    (game/add-player opponent my-name)
                    (game/add-cards my-name ["AC" "KC" "JC"])
                    (game/add-cards opponent ["2D" "4D" "6D"])
                    (game/dealt-state)
                    (game/bid my-name 2) (game/bid opponent 0)
                    (game/play my-name "AC") (game/play opponent "2D")
                    (game/play my-name "KC") (game/play opponent "4D"))]
      ;; Setup initial state.
      (setup-game my-name from-ai to-ai)

      ;; Send state to AI and it will/should play the only card it has
      ;; left (JC).
      (>!! to-ai (state-to-ai state my-name))
      (is (= {:message "play:JC"} (<!! from-ai)))

      (let [cards-played (-> state (game/play my-name "JC") (game/play opponent "6D"))]
        ;; Game should be over, since AI has > MAX_POINTS.
        (is (game/game-over? cards-played))
        (is (= my-name (game/get-winner cards-played)))
        ;; Start a new game and have the opponent pass the bid.
        (let [new-state (-> cards-played game/restart (game/bid opponent 0))]
          (>!! to-ai (state-to-ai new-state my-name))
          (is (= {:message "bid:2"} (<!! from-ai)))))

      (future-cancel brain))))

(deftest test-ai-start
  ;; There once existed a bug who only showed its face when:
  ;;  1) a player had requested an AI opponent, but
  ;;  2) the player left without starting the game.
  ;; Now the AI "owned" the game and would be responsible for starting it (if
  ;; the same player rejoined or another joined) and it had no logic to do so.
  (testing "AI can start a game"
    (let [my-name "AI" opponent "opponent"
          from-ai (chan) to-ai (chan)
          brain (future (play from-ai to-ai))
          state (-> game/empty-state
                    ;; AI is first player
                    (game/add-player my-name opponent))]
      ;; Setup initial state.
      (setup-game my-name from-ai to-ai)

      (>!! to-ai (state-to-ai state my-name))
      (is (= {:message "start:"} (<!! from-ai)))

      (future-cancel brain))))
