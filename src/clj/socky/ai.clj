(ns socky.ai
  (:require [clojure.core.async :refer [<!! >!! chan]]
            [socky.game :as game]))

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
      (when-let [msg (<!! out)]
        (reset! state (read-string msg))
        (recur)))
    (println "AI stopped due to disconnect")))
