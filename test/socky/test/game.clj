(ns socky.test.game
  (:use clojure.test
        socky.cards
        socky.game))

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

(deftest test-max-bid
  (testing "getting maximum bid out of all bids"
    (is (= (max-bid [1 2 3 0]) 3))
    (is (= (max-bid [0 0 0 2]) 2))
    (is (= (max-bid [3 0 0 0]) 3))
    (is (= (max-bid [0 2]) 2))
    (is (= (max-bid []) 0))))

(deftest test-highest-bidder
  (testing "seeing who was the highest bidder"
    (let [base (-> empty-state
                   (add-player "mike" "paul" "rob" "bob"))]
      (is (= (highest-bidder (assoc base :bids [0 0 2 3])) "bob"))
      (is (= (highest-bidder (assoc base :bids [3 0 0 0])) "mike"))
      (is (= (highest-bidder (assoc base :bids [0 0 3 0])) "rob"))
      (is (= (highest-bidder (assoc base :bids [0 3 0 0])) "paul")))))

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
      (= (get-table-cards state) ["4C"]))))

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
      (is (highest cards "C") "4C")
      (is (highest cards "D") "5D")
      (is (highest cards "S") "AS")
      (is (nil? (highest cards "H"))))))

(def hand-in-progress (-> empty-state
                          (add-player "tim" "louise" "sharon")
                          (add-cards "tim" ["4C" "5S" "7D"])
                          (add-cards "louise" ["AC" "KC" "9D"])
                          (add-cards "sharon" ["2C" "7S" "JC"])
                          (dealt-state "sharon")
                          (bid "tim" 2)
                          (bid "louise" 3)
                          (bid "sharon" 0)
                          (play "louise" "AC")
                          (play "sharon" "2C")))
(def hand-played (play hand-in-progress "tim" "4C"))

(deftest test-award-trick-to-winner
  (testing "put table-cards into winner's tricks pile"
    (is (= (get-player-tricks hand-played "louise") [["AC" "2C" "4C"]]))))

(deftest test-check-hand-winner
  (testing "if everyone has played a card, resolve state"
    ;; leave an unfinished hand alone
    (is (= hand-in-progress (check-hand-winner hand-in-progress "sharon")))
    ;; correctly sort out state for completed hand
    (let [resolved (check-hand-winner hand-played "tim")]
      ;; onus is on the winner
      (is (:onus resolved) "louise")
      ;; louise got the trick
      (is (= (get-player-tricks resolved "louise") [["AC" "2C" "4C"]]))
      ;; table cards were cleared
      (is (empty? (get-table-cards resolved))))))

(deftest test-game-play
  (testing "actual game play"
    (let [initial-state (-> empty-state
                            (add-player "tim" "sharon" "louise" "rob")
                            (add-cards)
                            (dealt-state))]
      (is (= (:dealer initial-state) "tim"))
      (is (= (:onus initial-state) "sharon"))
      ;; only sharon can make the next move
      (is (nil? (advance-state initial-state "louise" "bid" 2)))
      ;; bid must be at least 2
      (is (nil? (advance-state initial-state "sharon" "bid" 1)))
      ;; bid must not be greater than 4
      (is (nil? (advance-state initial-state "sharon" "bid" 5)))
      (let [next-state (advance-state initial-state "sharon" "bid" 2)]
        (is (= (:onus next-state) "louise"))
        (is (= (:bids next-state) [2]))
        ;; only louise can act
        (is (nil? (advance-state next-state "rob" "bid" "3")))
        ;; louise can only bid
        (is (nil? (advance-state next-state "louise" "play" "4C")))
        ;; louise must bid more than sharon
        (is (nil? (advance-state next-state "louise" "bid" 2)))
        (let [bid-3 (advance-state next-state "louise" "bid" 3)]
          ;; louise can bid more than sharon
          (is (not (nil? bid-3)))
          ;; then the onus should be on rob
          (is (= (:onus bid-3) "rob"))
          (is (= (:bids bid-3) [2 3])))
        (let [pass (advance-state next-state "louise" "bid" 0)]
          ;; louise can pass (implemented as bidding 0)
          (is (not (nil? pass)))
          ;; onus should still be on rob
          (is (= (:onus pass) "rob"))
          (is (= (:bids pass) [2 0]))
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
              (is (= (:onus play) "rob"))
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
                (is (= (get-suit rob-card) (:trump trump-lead)))
                ;; and that card is now on the table
                (is (= [rob-card] (:table-cards trump-lead)))))))))))
