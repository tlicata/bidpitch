(ns socky.test.ai
  (:use clojure.test socky.ai)
  (:require [clojure.core.async :refer [<!! >!! chan]]
            [socky.game :as game]))

(deftest test-possible-moves
  (let [my-name "AI" opponent "opponent-name"
        state (-> game/empty-state
                  (game/add-player my-name opponent)
                  (game/add-cards my-name ["AC" "KC" "JC"])
                  (game/add-cards opponent ["2D" "4D" "6D"])
                  (game/dealt-state))]
    (testing "possible bids"
      (is (= (possible-moves state) [{:action "bid" :value 4}
                                     {:action "bid" :value 3}
                                     {:action "bid" :value 2}
                                     {:action "bid" :value 0}]))
      (is (= (possible-moves (-> state (game/bid opponent 0)))
             [{:action "bid" :value 4}
              {:action "bid" :value 3}
              {:action "bid" :value 2}])))
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
          (is (= 1 (ai-has-high-trump high)))
          (is (= 0 (won-or-lost-low high)))
          (is (= 0 (won-or-lost-jack high))))
        (let [low (-> state
                      (game/play my-name "JC") (game/play opponent "2D"))]
          (is (= 1 (ai-has-high-trump low)))
          (is (= 1 (won-or-lost-low low)))
          (is (= 1 (won-or-lost-jack low)))))

      ;; Opponent picks trump.
      (let [state (-> base (game/bid opponent 2) (game/bid my-name 0))]
        (let [high (-> state
                       (game/play opponent "4D") (game/play my-name "KC"))]
          (is (= -1 (ai-has-high-trump high)))
          (is (= 0 (won-or-lost-low high)))
          (is (= 0 (won-or-lost-jack high))))
        (let [low (-> state
                      (game/play opponent "2D") (game/play my-name "JC"))]
          (is (= -1 (ai-has-high-trump low)))
          (is (= -1 (won-or-lost-low low)))
          (is (= 0 (won-or-lost-jack low))))))))

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
        (is (= 1 (static-score (-> state (game/play my-name "AC")
                                   (game/play opponent "2D")))))
        ;; 3 points for high, low, and jack.
        (is (= 3 (static-score (-> state (game/play my-name "JC")
                                   (game/play opponent "2D"))))))
      ;; Opponent picks trump.
      (let [state (-> base (game/bid opponent 2) (game/bid my-name 0))]
        ;; -2 since opponent has high (in hand) and low (played).
        (is (= -2 (static-score (-> state (game/play opponent "2D")
                                    (game/play my-name "KC")))))
        ;; -1 since opponent has high (played).
        (is (= -1 (static-score (-> state (game/play opponent "6D")
                                    (game/play my-name "JC")))))
        ;; -1 since opponent has high (in hand).
        (is (= -1 (static-score (-> state (game/play opponent "4D")
                                    (game/play my-name "JC")))))))))

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
        (is (= {:message "bid:2"} (<!! from-ai)))

        (let [done (-> state
                       (game/bid opponent 0)
                       (game/bid my-name 2)
                       (game/play my-name "AC")
                       (game/play opponent "2D")
                       (game/play my-name "KC")
                       (game/play opponent "4D")
                       (game/play my-name "JC")
                       (game/play opponent "6D"))]

          ;; AI will currently pass if given the opportunity.
          (>!! to-ai (state-to-ai done my-name))
          (is (= {:message "bid:0"} (<!! from-ai)))))

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
