(ns bidpitch.test.scenarios
  (:use clojure.test
        bidpitch.cards
        bidpitch.game))

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

(def tied-game-pts (binding [*reconcile-end-game* false]
                     (-> empty-state
                         (add-player "tim" "sharon" "louise")
                         (add-cards "tim" ["AD" "2C" "3C"])
                         (add-cards "sharon" ["AH" "2S" "3S"])
                         (add-cards "louise" ["AC" "2D" "2H"])
                         (dealt-state)
                         (bid "sharon" 0)
                         (bid "louise" 0)
                         (bid "tim" 2)
                         ;; first hand
                         (play "tim" "AD")
                         (play "sharon" "2S")
                         (play "louise" "2D")
                         ;; second hand
                         (play "tim" "2C")
                         (play "sharon" "3S")
                         (play "louise" "AC")
                         ;; third hand
                         (play "louise" "2H")
                         (play "tim" "3C")
                         (play "sharon" "AH"))))
