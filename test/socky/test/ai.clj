(ns socky.test.ai
  (:use clojure.test socky.ai)
  (:require [clojure.core.async :refer [<!! >!! chan]]
            [socky.game :as game]))

(deftest test-ai
  (testing "AI interaction with server"
    (let [my-name "AI"
          from-ai (chan) to-ai (chan)
          state game/empty-state
          brain (future (play from-ai to-ai))]

      ;; Expect to receive "name" message from AI.  Every client
      ;; tries to register with a name and is accepted if the name
      ;; is available and rejected otherwise.
      (is (= {:message my-name} (<!! from-ai)))

      ;; We'll accept the name "AI". In this case the server usually
      ;; sends back a signed JWT, giving this client the privilege to
      ;; that name in case of future collisions. Really, we can send
      ;; back anything other than "taken".
      (>!! to-ai "your-name-was-accepted")

      ;; After accepting the client, we send them the current state of
      ;; the game (shielded so they can't see other players' cards).
      (>!! to-ai (prn-str (game/shield state my-name)))

      ;; Expect to receive the "join" message from the client saying
      ;; the AI wants to join the game.
      (is (= {:message "join"} (<!! from-ai))))))
