(ns socky.test.scenarios
  (:use clojure.test
        socky.cards
        socky.game))

(def dont-make-bid (-> empty-state
                       (add-player "tim" "sharon" "louise")
                       (add-cards "tim" ["AC" "KC" "JC"])
                       (add-cards "sharon" ["2D" "4D" "6D"])
                       (add-cards "louise" ["9C" "2S" "3S"])
                       (dealt-state)
                       (bid "sharon" 0)
                       (bid "louise" 3)
                       (bid "tim" 0)
                       ;; first hand
                       (play "louise" "9C")
                       (play "tim" "AC")
                       (play "sharon" "2D")
                       ;; second hand
                       (play "tim" "KC")
                       (play "sharon" "4D")
                       (play "louise" "2S")
                       ;; third hand
                       (play "tim" "JC")
                       (play "sharon" "6D")
                       (play "louise" "3S")))
