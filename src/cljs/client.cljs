(ns bidpitch.client
  (:require [chord.client :refer [ws-ch]]
            [cljs.core.async :refer [<! >! chan put!]]
            [cljs.reader :refer [read-string]]
            [clojure.string :refer [blank? join replace]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [bidpitch.cards :refer [get-rank get-suit ranks suits to-unicode]]
            [bidpitch.game :as game]
            [bidpitch.shield :as shield]
            [bidpitch.util :as util])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [bidpitch.cljs-macros :refer [defview]]))

(def host (.-host (.-location js/window)))
(def path (.-pathname (.-location js/window)))
(def websocket-url (str "ws://" host  path "/socky"))
(def websocket (atom (chan)))

(def game-state (atom game/empty-state))

(defn send-message [msg]
  (put! @websocket msg))

(defn display [show]
  (if show #js {} #js {:display "none"}))

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
  (let [html-class {:className (str "card-img" (if card "" " empty"))}
        card-url {:src (str "/img/cards/individual/" card ".svg?2.09")
                  ;; Force react to not reuse card <img>s. Safari was
                  ;; messing with the sizes of cards when setting the
                  ;; `src` attribute to a cached image.
                  :key card}]
    (if card
      (dom/img (clj->js (merge html-class card-url)))
      (dom/span (clj->js html-class)))))
(defview card-view
  (let [msg (str "play:" data)
        handler #(bidpitch.client.send-message msg)]
    (dom/span #js {:onClick handler :className "card"} (card-ui data))))

(defview hand-view
  (if-let [player-cards (shield/my-cards data)]
    (apply dom/div
           #js {:className (str "hand" (when (shield/my-turn? data) " onus"))}
           (om/build-all card-view (sort-hand player-cards)))
    (dom/div nil "")))

(defn msg-button [text msg show]
  (dom/button #js {:className "button"
                   :style (display show)
                   :onClick #(send-message msg)} text))

(defn bid-button [data val txt]
  (msg-button txt (str "bid:" val) (game/valid-bid? data (:me data) val)))
(defview bid-view
  (dom/div #js {:className "bids" :style (display (shield/my-turn-to-bid? data))}
           (bid-button data 0 "pass")
           (bid-button data 2 "2")
           (bid-button data 3 "3")
           (bid-button data 4 "4")))

(def history-play-regex (partial re-matches #"(.*)\splay\s(\S\S)"))
(def history-bid-regex (partial re-matches #"(.*)\sbid\s(\d)"))
(defn history-current-game [state]
  (let [msgs (reverse (game/get-messages state))
        plays (take-while history-play-regex msgs)
        bids (take-while history-bid-regex (drop-while history-play-regex msgs))]
    (concat (reverse bids) (reverse plays))))
(defn history-unicode [msg]
  (if-let [[_ name card] (history-play-regex msg)]
    (replace msg card (to-unicode card))
    msg))
(defn history-pass [msg]
  (let [[_ name bid] (history-bid-regex msg)]
    (if (= bid "0") (replace msg "bid 0" "pass") msg)))
(defn history-personalize [person msg]
  (replace msg person "You"))
(defn history-pprint [state]
  (join "\n" (map (comp history-pass history-unicode
                        (partial history-personalize (shield/who-am-i state)))
                  (history-current-game state))))
(defn history-view [data]
  (if (empty? (game/get-messages data))
    (dom/span nil nil)
    (dom/span #js {:className "button history"
                   :onClick #(.alert js/window (history-pprint data))} "^")))

(defview start-view
  (let [players (game/get-players data)
        me (:me data)
        is-leader (= me (first players))
        started (game/game-started? data)
        can-join (game/can-join? data me)
        can-leave (game/can-leave? data me)
        can-start (and is-leader (not started) (> (count players) 1))]
    (dom/div #js {:className "start-view"}
             ;; hidden history button so can-start message is centered
             (dom/span #js {:style #js {:visibility "hidden"}} (history-view data))
             (if can-start
               (dom/span #js {:className "starter"}
                         (dom/p nil "When you're satisfied with the participant list,")
                         (msg-button "Start" "start" true))
               (let [show-ai (and is-leader (game/can-join? data "ai"))]
                 (dom/span (when show-ai #js {:className "show-ai"})
                           (dom/p nil (or (game/message-next-step data)
                                          (history-unicode (last (game/get-messages data)))))
                           (when show-ai (msg-button "Add AI Player" "ai" true)))))
             (history-view data))))

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

(defn name-ui [player-name]
  (dom/div #js {:className "player-wrapper"}
           (dom/span #js {:className "player"} player-name)))

(defview table-card
  (dom/div nil (card-ui (second data)) (name-ui (first data))))
(defview table-cards-view
  (when (and (game/game-started? data)
             (not (game/bidding-stage? data))
             (not (game/game-over? data)))
    (let [players (game/get-players data)
          table-cards (game/get-table-cards data)]
      (apply dom/div #js {:className "tablecards"}
             (om/build-all table-card (util/map-all vector players table-cards))))))

(defview join-list-li
  (dom/li nil (if (nil? data) "_____" data)))
(defview join-list-view
  (let [players-and-nils (game/get-players-with-nils data)]
    (dom/div #js {:className "player-list" :style (display (not (game/game-started? data)))}
             (dom/h3 nil "Players")
             (apply dom/ol nil
                    (om/build-all join-list-li players-and-nils)))))

(defview game-view
  (dom/div #js {:className "game"}
           (dom/div #js {:className "top-ui"}
                    (om/build bid-view data)
                    (om/build table-cards-view data)
                    (om/build join-list-view data))
           (when-not (game/game-started? data)
             (om/build start-view data))
           (dom/div #js {:className "bottom-ui"}
                    (when (game/game-started? data)
                      (om/build start-view data))
                    (om/build hand-view data)
                    (om/build points-view data))))

(set! (.-onload js/window)
      (fn []
        (let [target (.getElementById js/document "content")
              name (or (.getItem js/localStorage "username")
                       (.prompt js/window "Enter your name"))]
          (if (blank? name)
            (.alert js/window "It works better if you enter a name. Refresh to try again.")
            (go
              (let [{:keys [ws-channel error]} (<! (ws-ch websocket-url))]
                (when-not error
                  (reset! websocket ws-channel)
                  (send-message name)
                  (let [{message :message} (<! @websocket)]
                    (if (= message "taken")
                      (.alert js/window "Name already taken. Refresh to try again.")
                      (do
                        (let [username (read-string message)]
                          (.setItem js/localStorage "username" username)
                          (set! (.-cookie js/document) (str "username=" username ";path=/;expires=Fri, 31 Dec 9999 23:59:59 GMT")))
                        (reset! game-state (read-string (:message (<! @websocket))))
                        (send-message "join")
                        (om/root game-view game-state {:target target})
                        (loop []
                          (when-let [msg (<! @websocket)]
                            (reset! game-state (read-string (:message msg)))
                            (recur)))
                        (.alert js/window "server disconnected")))))))))))
