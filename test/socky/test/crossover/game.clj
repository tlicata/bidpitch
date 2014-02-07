(ns socky.test.crossover.game
  (:use clojure.test
        socky.crossover.game))

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
  (testing "ordering players after dealer"
    (let [players ["tim" "paul" "mike" "rob"]]
      (is (= (order-players players "tim")
             ["paul" "mike" "rob" "tim"]))
      (is (= (order-players players "paul")
             ["mike" "rob" "tim" "paul"]))
      (is (= (order-players players "mike")
             ["rob" "tim" "paul" "mike"]))
      (is (= (order-players players "rob")
             ["tim" "paul" "mike" "rob"])))))

(deftest test-max-bid
  (testing "getting maximum bid out of all bids"
    (is (= (max-bid [1 2 3 0]) 3))
    (is (= (max-bid [0 0 0 2]) 2))
    (is (= (max-bid [3 0 0 0]) 3))
    (is (= (max-bid [0 2]) 2))
    (is (= (max-bid []) 0))))

(deftest test-highest-bidder
  (testing "seeing who was the highest bidder"
    (let [base {:player-states [{:id "mike"} {:id "paul"} {:id "rob"} {:id "bob"}]}]
      (is (= (highest-bidder (assoc base :bids [0 0 2 3])) "bob"))
      (is (= (highest-bidder (assoc base :bids [3 0 0 0])) "mike"))
      (is (= (highest-bidder (assoc base :bids [0 0 3 0])) "rob"))
      (is (= (highest-bidder (assoc base :bids [0 3 0 0])) "paul")))))
