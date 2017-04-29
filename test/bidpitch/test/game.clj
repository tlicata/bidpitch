(ns bidpitch.test.game
  (:use clojure.test
        bidpitch.cards
        bidpitch.game
        bidpitch.shield
        bidpitch.test.scenarios))

(deftest test-deal
  (testing "dealing cards to players"
    (let [state (-> empty-state
                    (add-player "sharon")
                    (add-player "louise")
                    (add-cards)
                    (dealt-state))]
      (is (= (get-player-tricks state "sharon") []))
      (is (= (get-player-tricks state "louise") []))
      (is (= (count (get-player-cards state "sharon")) 6))
      (is (= (count (get-player-cards state "louise")) 6)))))

(deftest test-next-player
  (testing "next player functionality"
    (let [players ["tim" "louise" "sharon"]]
      (is (= (player-index players "tim") 0))
      (is (= (player-index players "louise") 1))
      (is (= (player-index players "sharon") 2))
      (is (= (next-player players "tim") "louise"))
      (is (= (next-player players "louise") "sharon"))
      (is (= (next-player players "sharon") "tim")))))

(deftest test-order-players
  (testing "ordering players from a starting point"
    (let [players ["tim" "paul" "mike" "rob"]]
      (is (= (order-players players "tim")
             ["tim" "paul" "mike" "rob"]))
      (is (= (order-players players "paul")
             ["paul" "mike" "rob" "tim"]))
      (is (= (order-players players "mike")
             ["mike" "rob" "tim" "paul"]))
      (is (= (order-players players "rob")
             ["rob" "tim" "paul" "mike"])))))

(deftest test-has-player
  (testing "detecting if a player has joined the game"
    (let [players ["carl" "alex"]
          state (-> empty-state (add-players players))]
      (is (nil? (has-player? empty-state "carl")))
      (is (= true  (has-player? state "carl")))
      (is (nil? (has-player? state "who"))))))
(deftest test-add-player
  (testing "add a NEW player ONLY before game has started"
    (let [players ["carl" "alex"]
          state (-> empty-state (add-players players))
          started (-> state add-cards dealt-state)]
      (is (= (get-players state) players))
      (let [joined (add-player state "tim")
            failed (add-player started "tim")
            failtoo (add-player state "alex")]
        (is (= (get-players joined) ["carl" "alex" "tim"]))
        (is (= (count (get-players failed)) (count players)))
        (is (= (count (get-players failtoo)) (count players)))))))
(deftest test-remove-player
  (testing "remove a player ONLY before game has started"
    (let [players ["bob" "anne" "rob" "leah"]
          state (-> empty-state (add-players players))]
      (let [minus (remove-player state "rob")]
        (is (= (get-players minus) ["bob" "anne" "leah"])))
      (is (nil? (remove-player state "joe")))
      (let [started (-> state (add-cards) (dealt-state))
            cant-remove (remove-player started "bob")]
        (is (nil? cant-remove))))))

(deftest test-shield
  (testing "hide opponent/teammate data from a player"
    (let [state (-> empty-state
                    (add-player "tim" "sharon" "louise")
                    (add-cards)
                    (dealt-state))]
      (is (nil? (who-am-i state)))
      (is (not (empty? (get-player-cards state "tim"))))
      (is (not (empty? (get-player-cards state "sharon"))))
      (is (not (empty? (get-player-cards state "louise"))))
      (let [hide-cards (shield state "tim" false)]
        (is (= "tim" (who-am-i hide-cards)))
        (is (not (empty? (get-player-cards hide-cards "tim"))))
        (is (nil? (get-player-cards hide-cards "sharon")))
        (is (nil? (get-player-cards hide-cards "louise"))))
      (let [all-cards (shield state "tim" true)]
        (is (= "tim" (who-am-i all-cards)))
        (is (not (empty? (get-player-cards all-cards "tim"))))
        (is (not (empty? (get-player-cards all-cards "sharon"))))
        (is (not (empty? (get-player-cards all-cards "louise"))))))))

(def biddy (-> empty-state (add-player "tim" "sharon" "louise" "bill") dealt-state))
(def bid-ex0 (-> biddy (bid "sharon" 2) (bid "louise" 3) (bid "bill" 0) (bid "tim" 0)))
(def bid-ex1 (-> biddy (bid "sharon" 0) (bid "louise" 2) (bid "bill" 0) (bid "tim" 0)))
(def bid-ex2 (-> biddy (bid "sharon" 0) (bid "louise" 0) (bid "bill" 3) (bid "tim" 0)))
(def bid-ex3 (-> biddy (bid "sharon" 0) (bid "louise" 2)))

(deftest test-max-bid
  (testing "getting maximum bid out of all bids"
    (is (= 3 (max-bid bid-ex0)))
    (is (= 2 (max-bid bid-ex1)))
    (is (= 3 (max-bid bid-ex2)))
    (is (= 2 (max-bid bid-ex3)))
    (is (= 0 (max-bid empty-state)))))

(deftest test-highest-bidder
  (testing "seeing who was the highest bidder"
    (is (= (highest-bidder bid-ex0) "louise"))
    (is (= (highest-bidder bid-ex1) "louise"))
    (is (= (highest-bidder bid-ex2) "bill"))
    (is (= (highest-bidder bid-ex3) "louise"))
    (is (nil? (highest-bidder empty-state)))))

(deftest test-valid-bid
  (testing "is a given bid allowed"
    (let [base (-> empty-state (add-player "tim" "sharon" "louise")
                   (add-cards) (dealt-state))]
      ;; tim is dealer, so sharon can lead and bid anything
      (is (valid-bid? base "sharon" 0))
      (is (valid-bid? base "sharon" 2))
      (is (valid-bid? base "sharon" 3))
      (is (valid-bid? base "sharon" 4))
      ;; sharon can't bid invalid values
      (is (not (valid-bid? base "sharon" -1)))
      (is (not (valid-bid? base "sharon" 1)))
      (is (not (valid-bid? base "sharon" 5)))
      ;; no one else can bid
      (is (not (valid-bid? base "tim" 2)))
      (is (not (valid-bid? base "louise" 2)))
      (is (not (valid-bid? base "random" 2)))
      ;; different sharon bid scenarios
      (let [sharon-pass (-> base (bid "sharon" 0))
            sharon-2 (-> base (bid "sharon" 2))
            sharon-3 (-> base (bid "sharon" 3))
            sharon-4 (-> base (bid "sharon" 4))]
        ;; louise can bid anything if sharon passed
        (is (valid-bid? sharon-pass "louise" 0))
        (is (valid-bid? sharon-pass "louise" 3))
        ;; louise can't bid invalid values
        (is (not (valid-bid? sharon-2 "louise" 1)))
        (is (not (valid-bid? sharon-4 "louise" 5)))
        ;; tim can't bid anything
        (is (not (valid-bid? sharon-pass "tim" -1)))
        (is (not (valid-bid? sharon-2 "tim" 2)))
        (is (not (valid-bid? sharon-3 "tim" 0)))
        ;; louise can bid more than sharon
        (is (valid-bid? sharon-pass "louise" 2))
        (is (valid-bid? sharon-2 "louise" 3))
        (is (valid-bid? sharon-3 "louise" 4))
        ;; louise can't bid <= sharon
        (is (not (valid-bid? sharon-2 "louise" 2)))
        (is (not (valid-bid? sharon-3 "louise" 2)))
        (is (not (valid-bid? sharon-4 "louise" 3)))
        ;; different louise bid scenarios
        (let [louise-duck (-> sharon-2 (bid "louise" 0))
              louise-jump (-> sharon-2 (bid "louise" 3))
              all-pass (-> sharon-pass (bid "louise" 0))]
          ;; tim can outbid all existing bids
          (is (valid-bid? louise-duck "tim" 3))
          (is (valid-bid? louise-jump "tim" 4))
          (is (valid-bid? all-pass "tim" 2))
          ;; tim can't bid <= than existing
          (is (not (valid-bid? louise-duck "tim" 2)))
          (is (not (valid-bid? louise-jump "tim" 2)))
          ;; dealer *must* bid if no one else has
          (is (not (valid-bid? all-pass "tim" 0))))))))

(def cards {:player-cards {"tim" {:cards ["5D" "6H" "AS" "3C" "JH"]}
                           "sharon" {:cards ["2C" "3D" "7C" "9H" "TS"]}
                           "louise" {:cards ["QH" "5S" "KS" "8H" "4D"]}}})

(deftest test-player-has-card
  (testing "whether or not a player has a card"
    (is (player-has-card? cards "tim" "5D"))
    (is (player-has-card? cards "tim" "JH"))
    (is (not (player-has-card? cards "tim" "KC")))
    (is (not (player-has-card? cards "tim" "3D")))
    (is (player-has-card? cards "sharon" "2C"))
    (is (player-has-card? cards "sharon" "3D"))
    (is (not (player-has-card? cards "sharon" "QH")))
    (is (not (player-has-card? cards "sharon" "KS")))
    (is (not (player-has-card? cards "sharon" "8H")))
    (is (player-has-card? cards "louise" "QH"))
    (is (player-has-card? cards "louise" "4D"))))

(deftest test-valid-play
  (testing "if a given card is valid play"
    ;; leading cards are always valid
    (is (valid-play? (assoc cards :table-cards []) "tim" "5D"))
    (is (valid-play? (assoc cards :table-cards []) "tim" "AS"))
    ;; unless they don't have that card
    (is (not (valid-play? (assoc cards :table-cards []) "tim" "4D")))
    ;; following suits is always valid
    (is (valid-play? (assoc cards :table-cards ["AD"]) "tim" "5D"))
    (is (valid-play? (assoc cards :table-cards ["AC"]) "tim" "3C"))
    (let [state (assoc cards :trump "D" :table-cards ["AC"])]
      ;; not following suit when able is an error
      (is (not (valid-play? state "tim" "6H")))
      (is (not (valid-play? state "sharon" "TS")))
      ;; throwing trump is ok though
      (is (valid-play? state "tim" "5D"))
      (is (valid-play? state "sharon" "3D"))
      (is (valid-play? state "louise" "4D"))
      ;; not following suit when can't is fine
      (is (valid-play? state "louise" "8H")))))

(deftest test-remove-card
  (testing "removing a card from a player's hand"
    (let [cards ["4C" "7H" "8D" "TS" "JC"]
          state (-> empty-state
                    (add-player "tim")
                    (add-cards "tim" cards))]
      (is (player-has-card? state "tim" "4C"))
      (is (= cards (get-player-cards state "tim")))
      (let [removed (remove-card state "tim" "4C")]
        (is (not (player-has-card? removed "tim" "4C")))
        (is (= ["7H" "8D" "TS" "JC"]
               (get-player-cards removed "tim")))))))

(deftest test-add-table-card
  (testing "playing a card to the table"
    (let [state (add-table-card empty-state "4C")]
      (is (= (get-table-cards state) ["4C"]))
      (let [again (add-table-card state "7S")]
        (is (= (get-table-cards again) ["4C" "7S"]))))))

(deftest test-clear-table-cards
  (testing "clearing the table after a hand"
    (let [state (-> empty-state
                    (add-table-card "4C")
                    (add-table-card "AS")
                    (add-table-card "QH")
                    (clear-table-cards))]
      (is (empty? (get-table-cards state))))))

(deftest test-highest
  (testing "finding the highest played card of a suit"
    (let [cards ["4C" "5D" "JD" "KS" "AS"]]
      (is (= (highest cards "C") "4C"))
      (is (= (highest cards "D") "JD"))
      (is (= (highest cards "S") "AS"))
      (is (nil? (highest cards "H"))))))
(deftest test-lowest
  (testing "finding the lowest played card of a suit"
    (let [cards ["7S" "JD" "KD" "AS" "2C"]]
      (is (= (lowest cards "C") "2C"))
      (is (= (lowest cards "S") "7S"))
      (is (= (lowest cards "D") "JD"))
      (is (nil? (lowest cards "H"))))))

(def hand-in-progress (-> empty-state
                          (add-player "tim" "louise" "sharon")
                          (add-cards "tim" ["4C" "5S" "7D" "QD"])
                          (add-cards "louise" ["AC" "KC" "4D" "TH"])
                          (add-cards "sharon" ["2C" "7S" "JC" "KS"])
                          (dealt-state "sharon")
                          (bid "tim" 2)
                          (bid "louise" 3)
                          (bid "sharon" 0)
                          (play "louise" "AC")
                          (play "sharon" "2C")))
(def hand-played (play hand-in-progress "tim" "4C"))
(def non-bidder-lead (-> hand-played
                          (play "louise" "4D")
                          (play "sharon" "7S")
                          (play "tim" "7D")
                          (play "tim" "5S")
                          (play "louise" "KC")
                          (play "sharon" "JC")))
(def game-pts (binding [*reconcile-end-game* false]
                (-> non-bidder-lead
                    (play "louise" "TH")
                    (play "sharon" "KS")
                    (play "tim" "QD"))))

(deftest test-award-hand-to-winner
  (testing "put table-cards into winner's tricks pile"
    (is (= (get-player-tricks hand-played "louise") [["AC" "2C" "4C"]]))))

(deftest test-check-hand-over
  (testing "if everyone has played a card, resolve state"
    ;; leave an unfinished hand alone
    (is (= hand-in-progress (check-hand-over hand-in-progress)))
    ;; correctly sort out state for completed hand
    (let [resolved (check-hand-over hand-played)]
      ;; onus is on the winner
      (is (= (get-onus resolved) "louise"))
      ;; louise got the trick
      (is (= (get-player-tricks resolved "louise") [["AC" "2C" "4C"]]))
      ;; table cards were cleared
      (is (empty? (get-table-cards resolved))))
    ;; non-bidder leads were causing trouble
    (is (not (nil? non-bidder-lead)))
    (is (= (get-player-tricks non-bidder-lead "tim") [["4D" "7S" "7D"]]))
    (is (= (get-player-tricks non-bidder-lead "louise") [["AC" "2C" "4C"] ["5S" "KC" "JC"]]))))

(deftest test-tally-game-pts
  (testing "adding up points for game"
    (is (= (tally-game-pts game-pts "tim") 0))
    (is (= (tally-game-pts game-pts "louise") 23))
    (is (= (tally-game-pts game-pts "sharon") 0))
    (is (= (tally-game-pts tied-game-pts "tim") 4))
    (is (= (tally-game-pts tied-game-pts "sharon") 4))
    (is (= (tally-game-pts tied-game-pts "louise") 4))))
(deftest test-most-game-pts
  (testing "which player earned the most game points"
    (is (= (most-game-pts game-pts) "louise"))
    (is (nil? (most-game-pts empty-state)))
    (is (nil? (most-game-pts tied-game-pts)))))
(deftest test-get-all-cards
  (testing "get all cards yet to be played"
    (is (= (sort (get-all-cards hand-in-progress))
           (sort ["4C" "5S" "7D" "QD" "KC" "4D" "TH" "7S" "JC" "KS"])))))
(deftest test-get-all-tricks
  (testing "retrieving all cards played during a round"
    (is (= (sort (get-all-tricks game-pts))
           (sort ["4C" "5S" "7D" "QD" "AC" "KC" "4D" "TH" "2C" "7S" "JC" "KS"])))))
(deftest test-who-won-card
  (testing "who won the card in question"
    (is (= (who-won-card game-pts "4D") "tim"))
    (is (= (who-won-card game-pts "7S") "tim"))
    (is (= (who-won-card game-pts "7D") "tim"))
    (is (= (who-won-card game-pts "AC") "louise"))
    (is (= (who-won-card game-pts "2C") "louise"))
    (is (= (who-won-card game-pts "4C") "louise"))
    (is (= (who-won-card game-pts "5S") "louise"))
    (is (= (who-won-card game-pts "KC") "louise"))
    (is (= (who-won-card game-pts "JC") "louise"))
    (is (= (who-won-card game-pts "QH") nil))))
(deftest test-round-over
  (testing "determining if all cards have been played in a round"
    (is (not (round-over? hand-in-progress)))
    (is (not (round-over? hand-played)))
    (is (not (round-over? non-bidder-lead)))
    (is (round-over? game-pts))))
(deftest test-game-over
  (testing "has someone won the game"
    (is (not (game-over? {:points {"tim" 0 "louise" 0 "sharon" 0}})))
    (is (not (game-over? {:points {"tim" 0 "louise" 0 "sharon" -3}})))
    (is (not (game-over? {:points {"tim" 5 "louise" 8 "sharon" 10}})))
    (is (game-over? {:points {"tim" 5 "louise" 8 "sharon" 11}}))
    (is (game-over? {:points {"tim" 5 "louise" 13 "sharon" 11}}))
    (is (not (game-over? {:points {"tim" 5 "louise" 11 "sharon" 11}})))))
(deftest test-declare-winner
  (testing "indicating who won the game"
    (is (= "tim" (:winner (declare-winner {:points {"tim" 11 "louise" 10 "sharon" -3}}))))
    (is (= "louise" (:winner (declare-winner {:points {"tim" 11 "louise" 12 "sharon" -3}}))))
    (is (= "sharon" (:winner (declare-winner {:points {"tim" 11 "louise" 12 "sharon" 13}}))))
    (is (nil? (:winner (declare-winner {:points {"tim" 1 "louise" 2 "sharon" 3}}))))
    (is (nil? (:winner (declare-winner {:points {"tim" 11 "louise" 11 "sharon" 11}}))))))
(deftest test-calc-points
  (testing "adding up scores from a round"
    (is (= (:points game-pts) {"louise" 4 "tim" 0 "sharon" 0}))
    (is (= (:points dont-make-bid) {"tim" 4 "sharon" 0 "louise" -3}))))

(deftest test-game-play
  (testing "actual game play"
    (let [initial-state (-> empty-state
                            (add-player "tim" "sharon" "louise" "rob")
                            (add-cards)
                            (dealt-state))]
      (is (= (get-dealer initial-state) "tim"))
      (is (= (get-onus initial-state) "sharon"))
      (is (= (get-messages initial-state) []))
      (is (= (get-log initial-state) []))
      ;; only sharon can make the next move
      (is (nil? (advance-state initial-state "louise" "bid" 2)))
      ;; bid must be at least 2
      (is (nil? (advance-state initial-state "sharon" "bid" 1)))
      ;; bid must not be greater than 4
      (is (nil? (advance-state initial-state "sharon" "bid" 5)))
      (let [next-state (advance-state initial-state "sharon" "bid" 2)]
        (is (= (get-onus next-state) "louise"))
        (is (= (get-bids next-state) {"sharon" 2}))
        (is (= (get-messages next-state) ["sharon bid 2"]))
        (is (= (get-log next-state) [{:player "sharon" :action "bid" :value 2}]))
        ;; only louise can act
        (is (nil? (advance-state next-state "rob" "bid" "3")))
        ;; louise can only bid
        (is (nil? (advance-state next-state "louise" "play" "4C")))
        ;; louise must bid more than sharon
        (is (nil? (advance-state next-state "louise" "bid" 2)))
        (let [bid-3 (advance-state next-state "louise" "bid" 3)]
          (is (= (get-messages bid-3) ["sharon bid 2" "louise bid 3"]))
          (is (= (get-log bid-3) [{:player "sharon" :action "bid" :value 2}
                                  {:player "louise" :action "bid" :value 3}]))
          ;; louise can bid more than sharon
          (is (not (nil? bid-3)))
          ;; then the onus should be on rob
          (is (= (get-onus bid-3) "rob"))
          (is (= (get-bids bid-3) {"sharon" 2, "louise" 3})))
        (let [pass (advance-state next-state "louise" "bid" 0)]
          ;; louise can pass (implemented as bidding 0)
          (is (not (nil? pass)))
          ;; onus should still be on rob
          (is (= (get-onus pass) "rob"))
          (is (= (get-bids pass) {"sharon" 2, "louise" 0}))
          ;; rob must bid more than sharon
          (is (nil? (advance-state pass "rob" "bid" 2)))
          ;; rob can pass
          (is (not (nil? (advance-state pass "rob" "bid" 0))))
          (let [bid-4 (advance-state pass "rob" "bid" 4)]
            ;; rob can bid 3 or 4
            (is (not (nil? bid-4)))
            ;; tim can't bid less than 4
            (is (nil? (advance-state bid-4 "tim" "bid" 2)))
            ;; tim should pass
            (let [play (advance-state bid-4 "tim" "bid" 0)
                  rob-card (first (get-player-cards play "rob"))
                  tim-card (first (get-player-cards play "tim"))]
              ;; onus should now be on highest bidder (rob)
              (is (= (get-onus play) "rob"))
              ;; rob can't do any more bidding
              (is (nil? (advance-state play "rob" "bid" 4)))
              ;; no one else can play
              (is (nil? (advance-state play "tim" "play" tim-card)))
              ;; rob can't play a card that's not his
              (is (nil? (advance-state play "rob" "play" tim-card)))
              (let [trump-lead (advance-state play "rob" "play" rob-card)]
                ;; rob can play any card
                (is (not (nil? trump-lead)))
                ;; and that suit is now trump
                (is (= (get-suit rob-card) (get-trump trump-lead)))
                ;; and that card is now on the table
                (is (= [rob-card] (get-table-cards trump-lead)))))))))))
