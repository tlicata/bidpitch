(ns socky.ai
  (:require [clojure.core.async :refer [<!! >!! chan]]
            [socky.game :as game]))

(def from-server (atom (chan)))
(def to-server (atom (chan)))
(def state (atom {}))

(defn who-am-i [state]
  (:me state))
(defn my-turn? [state]
  (= (game/get-onus state) (who-am-i state)))
(defn my-turn-to-bid? [state]
  (and (my-turn? state) (game/bidding-stage? state)))
(defn my-turn-to-play? [state]
  (and (my-turn? state) (not (game/bidding-stage? state))))
(defn my-cards [state]
  (try
    (:cards (val (first (:player-cards state))))
    (catch Exception _ [])))

(defn play [in out]
  (reset! to-server in)
  (reset! from-server out)
  (>!! in {:message "AI"})
  (let [jwt (<!! out) game-state (<!! out)]
    (>!! in {:message "join"})
    (loop []
      (when-let [game-state (read-string (<!! out))]
        (reset! state game-state)
        (when (my-turn? game-state)
          (Thread/sleep 2000)
          (>!! in {:message
                   (if (my-turn-to-bid? game-state)
                     (str "bid:" (first (filter (partial game/valid-bid? game-state (who-am-i game-state)) [0 1 2 3 4])))
                     (str "play:" (rand-nth (filter (partial game/valid-play? game-state (who-am-i game-state)) (my-cards game-state)))))}))
        (recur)))
    (println "AI stopped due to disconnect")))
