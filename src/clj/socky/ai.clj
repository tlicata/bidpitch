(ns socky.ai
  (:require [clojure.core.async :refer [<!! >!! chan]]
            [socky.game :as game]
            [socky.shield :as shield]))

(def from-server (atom (chan)))
(def to-server (atom (chan)))
(def state (atom {}))

(defn play [in out]
  (reset! to-server in)
  (reset! from-server out)
  (>!! in {:message "AI"})
  (let [jwt (<!! out) game-state (<!! out)]
    (>!! in {:message "join"})
    (loop []
      (when-let [game-state (read-string (<!! out))]
        (reset! state game-state)
        (when (shield/my-turn? game-state)
          (Thread/sleep 2000)
          (>!! in {:message
                   (if (shield/my-turn-to-bid? game-state)
                     (str "bid:" (first (filter (partial game/valid-bid? game-state (shield/who-am-i game-state)) [0 1 2 3 4])))
                     (str "play:" (rand-nth (filter (partial game/valid-play? game-state (shield/who-am-i game-state)) (shield/my-cards game-state)))))}))
        (recur)))
    (println "AI stopped due to disconnect")))
