(ns bidpitch.client
  (:require [chord.client :refer [ws-ch]]
            [cljs.core.async :refer [<! >! chan put!]]
            [cljs.reader :refer [read-string]]
            [clojure.string :refer [blank? join replace]]
            [goog.net.cookies]
            [reagent.core :as reagent]
            [reagent.dom :as reagent-dom]
            [bidpitch.cards :refer [get-rank get-suit ranks suits to-unicode]]
            [bidpitch.game :as game]
            [bidpitch.shield :as shield]
            [bidpitch.util :as util])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [bidpitch.cljs-macros :refer [defview safely]]))

(def host (.-host (.-location js/window)))
(def path (.-pathname (.-location js/window)))
(def websocket-url (str "ws://" host  path "/socky"))

(defonce websocket (reagent/atom (chan)))
(defonce game-state (reagent/atom game/empty-state))

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
  (let [html-class {:class (str "card-img" (if card "" " empty"))}
        card-url {:src (str "/img/cards/individual/" card ".svg")
                  ;; Force react to not reuse card <img>s. Safari was
                  ;; messing with the sizes of cards when setting the
                  ;; `src` attribute to a cached image.
                  :key card}]
    (if card
      [:img (merge html-class card-url)]
      [:span html-class ""])))

(defview hand-view
  (if-let [player-cards (shield/my-cards data)]
    [:div {:class (str "hand" (when (shield/my-turn? data) " onus"))}
     (for [card (sort-hand player-cards)]
       (let [handler #(send-message (str "play:" card))]
         ^{:key card} [:span.card {:on-click handler} (card-ui card)]))]
    [:div]))

(defn link-button [text url show]
  [:a.button {:href url :style (display show)} text])
(defn msg-button [text msg show]
  [:button.button {:style (display show) :onClick #(send-message msg)} text])

(defn bid-button [data val txt]
  (msg-button txt (str "bid:" val) (game/valid-bid? data (:me data) val)))
(defview bid-view
  [:div.bids {:style (display (shield/my-turn-to-bid? data))}
   (bid-button data 0 "pass")
   (bid-button data 2 "2")
   (bid-button data 3 "3")
   (bid-button data 4 "4")])

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
    [:span]
    [:span.button.history {:onClick #(.alert js/window (history-pprint data))} "^"]))

(defn possessive-name [state]
  (when-let [onus (game/get-onus state)]
    (if (= onus (:me state)) "Your" (str onus "'s"))))

(defn message-next-step [state]
  (if (game/game-started? state)
    (if (game/game-over? state)
      (str "Game over. " (game/get-winner state) " wins.")
      (when (game/get-onus state)
        (if (game/bidding-stage? state)
          (str (possessive-name state) " turn to bid.")
          (str (possessive-name state) " turn to play."))))
    "Waiting for everyone to join"))

(defview start-view
  [:div.start-view
   ;; hidden history button so can-start message is centered
   [:span {:style {:visibility "hidden"}} (history-view data)]
   (if (shield/can-i-start? data)
     [:span.starter
      [:p "When you're satisfied with the participant list,"]
      (msg-button "Start" "start" true)]
     (let [show-ai (and (shield/am-i-leader? data) (game/can-join? data "ai"))]
       [:span (when show-ai {:class "show-ai"})
        [:p (or (message-next-step data)
                (history-unicode (last (game/get-messages data))))]
        (when show-ai (msg-button "Add AI Player" "ai" true))]))
   (history-view data)])

(defn points-li [[player points]]
  ^{:key player} [:li (str player ": " points)])
(defview points-view
  (let [points (:points data)
        winner (:winner data)]
    [:div.points {:style (display (not (empty? points)))}
     [:ul (for [point points] (points-li point))]
     [:div {:style (display (not (nil? winner)))} (str winner " wins!")]
     (msg-button "Play again!" "start" (game/game-over? data))
     (link-button "Home" "/" (game/game-over? data))]))

(defn name-ui [player-name]
  [:div.player-wrapper
   [:span.player player-name]])

(defn table-card [[player card]]
  ^{:key player} [:div (card-ui card) (name-ui player)])
(defview table-cards-view
  (when (and (game/game-started? data)
             (not (game/bidding-stage? data))
             (not (game/game-over? data)))
    (let [players (game/get-players data)
          table-cards (game/get-table-cards data)]
      [:div.tablecards
       (for [each-card (util/map-all vector players table-cards)]
         (table-card each-card))])))

(defn join-list-li [player index]
  ^{:key index} [:li (if (nil? player) "_____" player)])
(defview join-list-view
  (let [players-and-nils (game/get-players-with-nils data)]
    [:div.player-list {:style (display (not (game/game-started? data)))}
     [:h3 "Players"]
     [:ol (map-indexed (fn [index player]
                         (join-list-li player index))
                       players-and-nils)]]))

(defview game-view
  [:div.game
   [:div.top-ui
    [bid-view]
    [table-cards-view]
    [join-list-view]]
   (when-not (game/game-started? data) [start-view])
   [:div.bottom-ui
    (when (game/game-started? data) [start-view])
    [hand-view]
    [points-view]]])

(set! (.-onload js/window)
      (fn []
        (let [target (.getElementById js/document "content")
              name (or (.getItem js/localStorage "username")
                       (.get goog.net.cookies "username")
                       (.prompt js/window "Pick a username"))]
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
                          (safely (.setItem js/localStorage "username" username))
                          (set! (.-cookie js/document) (str "username=" username ";path=/;expires=Fri, 31 Dec 9999 23:59:59 GMT")))
                        (reset! game-state (read-string (:message (<! @websocket))))
                        (send-message "join")
                        (reagent-dom/render [game-view] target)
                        (loop []
                          (when-let [msg (<! @websocket)]
                            (reset! game-state (read-string (:message msg)))
                            (recur)))
                        (.alert js/window "Lost connection. Reload page.")))))))))))

(defn on-figwheel-reload []
  (reagent-dom/render [game-view] (.getElementById js/document "content")))
