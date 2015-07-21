(ns socky.view
  (:require [clojure.string :refer [join]]
            [hiccup.element :refer [link-to]]
            [hiccup.page :refer [html5 include-css include-js]]
            [socky.game :refer [game-started? get-players]]))

(def games-path "/games/")

(defn meta-viewport []
  [:meta {:name "viewport" :content "width=device-width, user-scalable=no"}])

(defn page-home [games]
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
                    (when-not (game-started? game)
                      (let [url (str games-path id)
                            players (get-players game)
                            names (if (empty? players) "[EMPTY]" (join "," players))]
                        [:li (link-to url (str "Join w/ " names))])))
                  games)]
     [:form {:action games-path :method "POST"}
      [:input {:type "submit" :value "Create a game"}]]]
    [:div.row3
     [:a.howto {:href "https://github.com/tlicata/bidpitch"} "What is this"]]
    [:div.row4
     [:a.howto {:href "http://en.wikipedia.org/wiki/Pitch_(card_game)"} "How to play"]
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
