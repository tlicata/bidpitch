(ns socky.client
  (:require [chord.client :refer [ws-ch]]
            [cljs.core.async :refer [<! >! chan put!]]
            [cljs.reader :refer [read-string]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [socky.cards :refer [get-rank get-suit ranks suits]]
            [socky.game :refer [bidding-stage? empty-state index-of]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(def websocket-url "ws://localhost:8080/socky")
(def websocket (atom (chan)))

(def state (atom empty-state))

(defn send-message [msg]
  (put! @websocket (or msg "bid:pass")))

(defn display [show]
  (if show
    #js {}
    #js {:display "none"}))

(defn sort-cards [card1 card2]
  (let [suit1 (get-suit card1)
        suit2 (get-suit card2)
        rank1 (get-rank card1)
        rank2 (get-rank card2)]
    (if (= suit1 suit2)
      (if (> (index-of ranks rank1)
             (index-of ranks rank2))
        1 -1)
      (if (> (index-of suits suit1)
             (index-of suits suit2))
        1 -1))))
(defn sort-hand [cards]
  (vec (sort sort-cards cards)))
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
          (let [cards (sort-hand (:cards (val player-cards)))]
            (dom/div
             #js {:className "hand"}
             (apply dom/ul nil (om/build-all card-view cards))))
          (dom/div nil ""))))))

(defn bid-view [data owner]
  (reify
    om/IRender
    (render [_]
      (let [my-bid (and (bidding-stage? data)
                        (= (:onus data) (:me data)))]
        (dom/p #js {:style (display my-bid)}
               "bid, you fool")))))

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
               (om/build bid-view data)
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
