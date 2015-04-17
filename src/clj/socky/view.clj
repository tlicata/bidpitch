(ns socky.view
  (:require [hiccup.page :refer [html5 include-css include-js]]))

(defn meta-viewport []
  [:meta {:name "viewport" :content "width=device-width"}])

(defn page-home []
  (html5
   [:head
    [:title "Bid Pitch - Home"]
    (include-css "/css/styles.css")
    (meta-viewport)]
   [:body.page.home
    [:div.row1
     [:h1 "Bid Pitch"]]
    [:div.row2
     [:form {:action "/games/" :method "POST"}
      [:input {:type "submit" :value "Play a game"}]]]
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
