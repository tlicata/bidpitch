(ns socky.client
  (:require [chord.client :refer [ws-ch]]
            [cljs.core.async :refer [<! >! chan put!]]
            [cljs.reader :refer [read-string]]
            [clojure.string :refer [blank?]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [socky.cards :refer [get-rank get-suit ranks suits]]
            [socky.game :as game]
            [socky.table :as table])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [socky.cljs-macros :refer [defview]]))

(def host (.-host (.-location js/window)))
(def path (.-pathname (.-location js/window)))
(def websocket-url (str "ws://" host  path "/socky"))
(def websocket (atom (chan)))

(def game-state (atom game/empty-state))

(defn send-message [msg]
  (put! @websocket msg))

(defn display [show]
  (if show #js {} #js {:display "none"}))

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
  (let [url (str "/img/cards/individual/" card ".svg?2")]
    (dom/img #js {:src url :className "card"})))
(defview card-view
  (let [msg (str "play:" data)
        handler #(socky.client.send-message msg)]
    (dom/li #js {:onClick handler} (card-ui data))))

(defview hand-view
  (if-let [player-cards (first (:player-cards data))]
    (let [cards (sort-hand (:cards (val player-cards)))
          class (str "hand" (if (my-turn? data) " onus" ""))]
      (dom/div #js {:className class}
               (apply dom/ul nil (om/build-all card-view cards))))
    (dom/div nil "")))

(defn msg-button [text msg show]
  (dom/button #js {:className "button"
                   :style (display show)
                   :onClick #(send-message msg)} text))
(defview start-view
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
             (msg-button "Start" "start" can-start)
             (msg-button "Join" "join" can-join)
             (msg-button "Leave" "leave" can-leave))))

(defn bid-button [data val txt]
  (msg-button txt (str "bid:" val) (game/valid-bid? data (:me data) val)))
(defview bid-view
  (dom/div #js {:style (display (my-turn-to-bid? data))}
           (bid-button data 0 "pass")
           (bid-button data 2 "2")
           (bid-button data 3 "3")
           (bid-button data 4 "4")))

(defview points-li
  (dom/li nil (str (key data) ": " (val data))))
(defview points-view
  (let [points (:points data)
        winner (:winner data)]
    (dom/div #js {:className "points"
                  :style (display (not (empty? points)))}
             (apply dom/ul nil (om/build-all points-li points))
             (dom/div #js {:style (display (not (nil? winner)))}
                      (str winner " wins!"))
             (msg-button "Play again!" "start" (game/game-over? data)))))

(defview table-card-li
  (dom/li nil (card-ui data)))
(defview table-cards-view
  (let [table-cards (game/get-table-cards data)]
    (apply dom/ul #js {:className "tablecards"
                       :style (display (seq table-cards))}
           (om/build-all table-card-li table-cards))))

(defview players-li
  (dom/li nil (if (nil? data) "_____" data)))
(defview players-view
  (let [players-and-nils (game/get-players-with-nils data)]
    (dom/div #js {:style (display (not (game/game-started? data)))}
             (dom/h3 nil "Players")
             (apply dom/ol nil
                    (om/build-all players-li players-and-nils)))))

(defview state-view
  (dom/p nil (prn-str data)))

(defview table-view
  (socky.table.render data))

(defview game-view
  (dom/div nil
           (om/build players-view data)
           (om/build start-view data)
           (om/build hand-view data)
           (om/build points-view data)
           (om/build bid-view data)
           (om/build table-cards-view data)
           (om/build table-view data)
           ;; (om/build state-view data)
           ))

(set! (.-onload js/window)
      (fn []
        (let [target (.getElementById js/document "content")
              name (or (.getItem js/localStorage "username")
                       (.prompt js/window "Enter your name"))]
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
                    (.setItem js/localStorage "username" name)
                    (send-message "join")
                    (om/root game-view game-state {:target target})
                    (loop []
                      (when-let [msg (<! @websocket)]
                        (reset! game-state (read-string (:message msg)))
                        (recur)))
                    (.alert js/window "server disconnected")))))))))
