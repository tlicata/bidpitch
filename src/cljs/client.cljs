(ns socky.client
  (:require [chord.client :refer [ws-ch]]
            [cljs.core.async :refer [<! >! chan put!]]
            [cljs.reader :refer [read-string]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [socky.game :as game])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(def websocket-url "ws://localhost:8080/socky")
(def websocket (atom (chan)))

(def state (atom game/empty-state))

(defn send-message [msg]
  (put! @websocket (or msg "bid:pass")))

(defn card-view [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/li nil data))))

(defn hand-view [data owner]
  (reify
    om/IRender
    (render [_]
      (let [player-cards (first (:player-cards data))]
        (if (not (nil? player-cards))
          (let [cards (vec (:cards (val player-cards)))]
            (apply dom/ul nil (om/build-all card-view cards)))
          (dom/div nil ""))))))

(set! (.-onload js/window)
      (fn []
        (let [target (.getElementById js/document "content")]
          (go
           (reset! websocket (<! (ws-ch websocket-url)))
           (om/root hand-view state {:target target})
           (send-message "state")
           (loop []
             (when-let [msg (<! @websocket)]
               (reset! state (read-string (:message msg)))
               (recur)))))))
