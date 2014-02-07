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
