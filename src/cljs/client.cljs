(ns socky.client
  (:require [chord.client :refer [ws-ch]]
            [cljs.core.async :refer [<! >! chan put!]]
            [cljs.reader :refer [read-string]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [socky.cards :refer [get-rank get-suit ranks suits]]
            [socky.game :as game])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(def websocket-url "ws://localhost:8080/socky")
(def websocket (atom (chan)))

(def game-state (atom game/empty-state))

(defn send-message [msg]
  (put! @websocket (or msg "bid:pass")))

(defn display [show]
  (if show
    #js {}
    #js {:display "none"}))

(defn my-turn? [state]
  (= (:onus state) (:me state)))
(defn my-turn-to-bid? [state]
  (and (my-turn? state) (game/bidding-stage? state)))
(defn my-turn-to-play? [state]
  (and (my-turn? state) (not (game/bidding-stage? state))))

(defn sort-cards [card1 card2]
  (let [suit1 (get-suit card1)
        suit2 (get-suit card2)
        rank1 (get-rank card1)
        rank2 (get-rank card2)]
    (if (= suit1 suit2)
      (if (> (game/index-of ranks rank1)
             (game/index-of ranks rank2))
        1 -1)
      (if (> (game/index-of suits suit1)
             (game/index-of suits suit2))
        1 -1))))
(defn sort-hand [cards]
  (vec (sort sort-cards cards)))
(defn card-view [card owner]
  (reify
    om/IRender
    (render [this]
      (let [url (str "url(/img/cards/individual/" card ".svg)")
            msg (str "play:" card)
            handler #(socky.client.send-message msg)]
        (dom/li #js {:style #js {:backgroundImage url}
                     :className "card"
                     :onClick handler})))))

(defn hand-view [data owner]
  (reify
    om/IRender
    (render [_]
      (let [player-cards (first (:player-cards data))]
        (if (not (nil? player-cards))
          (let [cards (sort-hand (:cards (val player-cards)))
                class (str "hand" (if (my-turn? data) " onus" ""))]
            (dom/div
             #js {:className class}
             (apply dom/ul nil (om/build-all card-view cards))))
          (dom/div nil ""))))))

(defn join-view [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/button #js {:style (display (game/can-join? data (:me data)))
                       :onClick #(send-message "join")}
                  "join"))))

(defn bid-button [data val txt]
  (dom/button #js {:style (display (game/valid-bid? data (:me data) val))
                   :onClick #(send-message (str "bid:" val))} txt))
(defn bid-view [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:style (display (my-turn-to-bid? data))}
               (bid-button data 0 "pass")
               (bid-button data 2 "2")
               (bid-button data 3 "3")
               (bid-button data 4 "4")))))

(defn points-li [data owner]
  (dom/li nil (str (key data) ": " (val data))))
(defn points-view [data owner]
  (reify
    om/IRender
    (render [_]
      (let [points (:points data)
            winner (:winner data)]
        (dom/div #js {:className "points"
                      :style (display (not (empty? points)))}
                 (apply dom/ul nil (om/build-all points-li points))
                 (dom/div #js {:style (display (not (nil? winner)))}
                           (str winner " wins!"))
                 (dom/button #js {:style (display (game/game-over? data))
                                  :onClick #(send-message "start")}
                             "New Game"))))))

(defn table-cards-view [data owner]
  (reify
    om/IRender
    (render [_]
      (let [table-cards (game/get-table-cards data)]
        (dom/p #js {:style (display (not (empty? table-cards)))}
               (prn-str table-cards))))))

(defn state-view [data owner]
  (reify
    om/IRender
    (render [_] (dom/p nil (prn-str data)))))

(defn game-view [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (om/build join-view data)
               (om/build hand-view data)
               (om/build points-view data)
               (om/build bid-view data)
               (om/build table-cards-view data)
               ;; (om/build state-view data)
               ))))

(set! (.-onload js/window)
      (fn []
        (let [target (.getElementById js/document "content")]
          (go
           (reset! websocket (<! (ws-ch websocket-url)))
           (om/root game-view game-state {:target target})
           (send-message "state")
           (loop []
             (when-let [msg (<! @websocket)]
               (reset! game-state (read-string (:message msg)))
               (recur)))))))
