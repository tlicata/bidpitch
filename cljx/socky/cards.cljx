(ns socky.cards)

(def ranks ["2" "3" "4" "5" "6" "7" "8" "9" "T" "J" "Q" "K" "A"])
(def suits ["S" "C" "H" "D"])

(defn get-suit [card]
  (str (second card)))

(defn get-rank [card]
  (str (first card)))

(defn create-deck []
  (shuffle (flatten (map #(map (fn [rank] (str rank %)) ranks) suits))))
