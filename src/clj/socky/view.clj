(ns socky.view
  (:require [clojure.string :refer [join]]
            [hiccup.element :refer [link-to]]
            [hiccup.page :refer [html5 include-css include-js]]
            [socky.game :refer [game-started? get-players]]))

(def games-path "/games/")

(defn meta-viewport []
  [:meta {:name "viewport" :content "width=device-width, user-scalable=no"}])

(defn page-home [games player]
  (html5
   [:head
    [:title "Bid Pitch - Home"]
    (include-css "/css/styles.css")
    (meta-viewport)]
   [:body.page.home
    [:div.row1
     [:h1 "Bid Pitch"]]
    [:div.row2
     `[:ul ~@(map (fn [[id game]]
                    (when-not (or (game-started? game) (empty? (get-players game)))
                      (let [url (str games-path id)
                            names (join "," (get-players game))]
                        [:li (link-to url (str "Join w/ " names))])))
                  games)]
     [:form {:action games-path :method "POST"}
      [:input {:type "submit" :value "Create a game"}]]]
    [:div.row4
     [:a.howto {:href "http://en.wikipedia.org/wiki/Pitch_(card_game)"} "Wikipedia"]
     [:p.small "(Hint: Auction Pitch with"]
     [:p.small "High, Low, Jack, and Game)"]]]))

(defn page-game [id]
  (html5
   [:head
    [:title "Bid Pitch"]
    (include-css "/css/styles.css")
    (include-js "/js/bin/main.js")
    (meta-viewport)]
   [:body
    [:div#content]]))
