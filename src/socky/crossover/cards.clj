(ns socky.crossover.cards)

(def ranks ["2" "3" "4" "5" "6" "7" "8" "9" "10" "J" "Q" "K" "A"])
(def suits ["S" "C" "H" "D"])

(defn create-deck []
  (shuffle (flatten (map #(map (fn [rank] (str rank %)) ranks) suits))))
