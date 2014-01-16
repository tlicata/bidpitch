(ns socky.crossover.cards)

(def ranks [:2 :3 :4 :5 :6 :7 :8 :9 :10 :J :Q :K :A])
(def suits [:S :C :H :D])

(defn deck []
  (flatten (map #(map (fn [rank] {:rank rank :suit %}) ranks) suits)))

(defn shuffled-deck []
  (shuffle (deck)))
