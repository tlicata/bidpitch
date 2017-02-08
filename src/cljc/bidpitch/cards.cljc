(ns bidpitch.cards)

(def ranks ["2" "3" "4" "5" "6" "7" "8" "9" "T" "J" "Q" "K" "A"])
(def suits ["C" "D" "S" "H"])

(defn get-suit [card]
  (str (second card)))

(defn get-rank [card]
  (str (first card)))

(defn make-card [rank suit]
  (str rank suit))

(defn create-deck []
  (shuffle (flatten (map #(map (fn [rank] (str rank %)) ranks) suits))))

(defn to-unicode [card]
  (let [suit (get-suit card)
        rank (get-rank card)]
    (str rank (condp = suit
                "C" "♣"
                "S" "♠"
                "D" "♦"
                "H" "♥"
                suit))))
