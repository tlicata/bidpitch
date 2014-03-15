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

(defn card-view [card owner]
  (reify
    om/IRender
    (render [this]
      (let [url (str "url(/img/cards/individual/" card ".svg)")
            play (str "play:" card)
            handler (fn [_] (socky.client.send-message play))]
        (dom/li #js {:style #js {:backgroundImage url}
                     :className "card"
                     :onClick handler})))))

(defn hand-view [data owner]
  (reify
    om/IRender
    (render [_]
      (let [player-cards (first (:player-cards data))]
        (if (not (nil? player-cards))
          (let [cards (vec (:cards (val player-cards)))]
            (dom/div
             #js {:className "hand"}
             (apply dom/ul nil (om/build-all card-view cards))))
          (dom/div nil ""))))))

(defn state-view [data owner]
  (reify
    om/IRender
    (render [_] (dom/p nil (prn-str data)))))

(defn game-view [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (om/build hand-view data)
               (om/build state-view data)))))

(set! (.-onload js/window)
      (fn []
        (let [target (.getElementById js/document "content")]
          (go
           (reset! websocket (<! (ws-ch websocket-url)))
           (om/root game-view state {:target target})
           (send-message "state")
           (loop []
             (when-let [msg (<! @websocket)]
               (reset! state (read-string (:message msg)))
               (recur)))))))
