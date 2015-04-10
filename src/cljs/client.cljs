(ns socky.client
  (:require [chord.client :refer [ws-ch]]
            [cljs.core.async :refer [<! >! chan put!]]
            [cljs.reader :refer [read-string]]
            [clojure.string :refer [blank?]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [socky.cards :refer [get-rank get-suit ranks suits]]
            [socky.game :as game])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(def host (.-host (.-location js/window)))
(def path (.-pathname (.-location js/window)))
(def websocket-url (str "ws://" host  path "/socky"))
(def websocket (atom (chan)))

(def game-state (atom game/empty-state))

(defn send-message [msg]
  (put! @websocket msg))

(defn display [show]
  (if show
    #js {}
    #js {:display "none"}))

(defn my-turn? [state]
  (= (game/get-onus state) (:me state)))
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

(defn card-ui [card]
  (let [url (str "url(/img/cards/individual/" card ".svg)")]
    (dom/span #js {:style #js {:backgroundImage url}
                   :className "card"})))
(defn card-view [card owner]
  (reify
    om/IRender
    (render [this]
      (let [msg (str "play:" card)
            handler #(socky.client.send-message msg)]
        (dom/li #js {:onClick handler} (card-ui card))))))

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

(defn start-view [data owner]
  (reify
    om/IRender
    (render [_]
      (let [players (game/get-players data)
            me (:me data)
            is-leader (= me (first players))
            started (game/game-started? data)
            can-join (game/can-join? data me)
            can-leave (game/can-leave? data me)
            can-start (and is-leader (not started) (> (count players) 1))]
        (dom/div #js {:style (display (not started))}
                 (dom/p nil (if can-start
                              "You're the leader, start when you're satisfied with the participant list."
                              (if can-join "" "Waiting for others to join...")))
                 (dom/button #js {:className "button"
                                  :style (display can-start)
                                  :onClick #(send-message "start")}
                             "Start")
                 (dom/button #js {:className "button"
                                  :style (display can-join)
                                  :onClick #(send-message "join")}
                             "Join")
                 (dom/button #js {:className "button"
                                  :style (display can-leave)
                                  :onClick #(send-message "leave")}
                             "Leave"))))))

(defn bid-button [data val txt]
  (dom/button #js {:className "button"
                   :style (display (game/valid-bid? data (:me data) val))
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
                 (dom/button #js {:className "button"
                                  :style (display (game/game-over? data))
                                  :onClick #(send-message "start")}
                             "Play again!"))))))

(defn table-card-li [data owner]
  (dom/li nil (card-ui data)))
(defn table-cards-view [data owner]
  (reify
    om/IRender
    (render [_]
      (let [table-cards (game/get-table-cards data)]
        (apply dom/ul #js {:className "tablecards"
                           :style (display (seq table-cards))}
               (om/build-all table-card-li table-cards))))))

(defn players-li [data owner]
  (dom/li nil (if (nil? data) "_____" data)))
(defn players-view [data owner]
  (reify
    om/IRender
    (render [_]
      (let [players-and-nils (game/get-players-with-nils data)]
        (dom/div #js {:style (display (not (game/game-started? data)))}
                 (dom/h3 nil "Players")
                 (apply dom/ol nil
                        (om/build-all players-li players-and-nils)))))))

(defn state-view [data owner]
  (reify
    om/IRender
    (render [_] (dom/p nil (prn-str data)))))

(defn game-view [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (om/build players-view data)
               (om/build start-view data)
               (om/build hand-view data)
               (om/build points-view data)
               (om/build bid-view data)
               (om/build table-cards-view data)
               ;; (om/build state-view data)
               ))))

(set! (.-onload js/window)
      (fn []
        (let [target (.getElementById js/document "content")
              name (.prompt js/window "Enter your name")]
          (if (blank? name)
            (.alert js/window "It works better if you enter a name. Refresh to try again.")
            (go
              (reset! websocket (<! (ws-ch websocket-url)))
              (send-message name)
              (let [{message :message} (<! @websocket)]
                (if (= message "taken")
                  (.alert js/window "Name already taken. Refresh to try again.")
                  (do
                    (reset! game-state (read-string message))
                    (send-message "join")
                    (om/root game-view game-state {:target target})
                    (loop []
                      (when-let [msg (<! @websocket)]
                        (reset! game-state (read-string (:message msg)))
                        (recur)))
                    (.alert js/window "server disconnected")))))))))
