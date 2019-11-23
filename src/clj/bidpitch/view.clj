(ns bidpitch.view
  (:require [clojure.string :refer [join]]
            [hiccup.element :refer [link-to]]
            [hiccup.page :refer [html5 include-css include-js]]
            [bidpitch.game :refer [game-started? get-players]]))

(def games-path "/games/")

(defn meta-viewport []
  [:meta {:name "viewport" :content "width=device-width, user-scalable=no"}])


(defn games-for-user [games username]
  (filter #(and (game-started? (val %)) (some #{username} (get-players (val  %)))) games))

(defn render-list-of-joined-games [games player]
  (let [ongoing (if player (games-for-user games player) [])]
    (when-not (empty? ongoing)
      [:div
       [:h5 "Rejoin:"]
       `[:ul
         ~@(map (fn [[id game]]
                  (let [url (str games-path id)
                        names (join "," (remove #{player} (get-players game)))]
                    [:li (link-to url (str "You & " names))]))
                ongoing)]])))

(defn render-list-of-games-to-join [games]
  (let [waiting (remove (fn [[id game]]
                          (or (game-started? game)
                              (empty? (get-players game))))
                        games)]
    (when-not (empty? waiting)
      [:div
       [:h5 "Join a game with:"]
       `[:ul
         ~@(map (fn [[id game]]
                  [:li (link-to (str games-path id) (join "," (get-players game)))])
                waiting)]])))

(defn page-home [games player]
  (html5
   [:head
    [:title "Bid Pitch"]
    (include-css "/css/styles.css")
    (meta-viewport)]
   [:body.page.home
    [:div.row1
     [:h1 "Bid Pitch"]]
    [:div.row2
     (render-list-of-joined-games games player)
     (render-list-of-games-to-join games)
     [:form {:action games-path :method "POST"}
      [:input {:type "submit" :value "Create game"}]]]
    [:div.row4
     [:a.howto {:href "http://en.wikipedia.org/wiki/Pitch_(card_game)"} "Wikipedia"]
     [:p.small "(Hint: Auction Pitch with"]
     [:p.small "High, Low, Jack, and Game)"]]]))

(defn page-game [id]
  (html5
   [:head
    [:title "Bid Pitch"]
    (include-css "/css/styles.css")
    (include-js "/cljs/app.js")
    (meta-viewport)]
   [:body
    [:div#content]]))

(defn page-not-found []
  (html5
   [:head
    [:title "Bid Pitch - Page Not Found"]
    (include-css "/css/styles.css")
    (meta-viewport)]
   [:body.page
    [:div.row1
     [:h1 "Bid Pitch"]]
    [:div.row2
     [:p "Page Not Found"]]
    [:div.row3
     [:form {:action "/" :method "GET"}
      [:input {:type "submit" :value "Home"}]]]]))

(defn page-rename []
  (html5
   [:head
    [:title "Bid Pitch"]
    (include-js "/js/rename.js")
    (meta-viewport)]
   [:body "Name cleared"]))
