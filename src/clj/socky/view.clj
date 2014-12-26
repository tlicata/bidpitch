(ns socky.view
  (:require [hiccup.page :refer [html5 include-css include-js]]
            [hiccup.element :refer [javascript-tag link-to]]
            [hiccup.util :refer [escape-html]]))

(defn button [link text]
  [:a.button {:href link} text])

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
     [:div (button "/games/new" "Start Game")]
     [:div (button "/games/" "Join Game")]]
    [:div.row3
     [:a.howto {:href "http://en.wikipedia.org/wiki/Pitch_(card_game)"} "How to play"]
     [:p.small "(Hint: Auction Pitch with"]
     [:p.small "High, Low, Jack, and Game)"]]]))

(defn page-game [id]
  (html5
   [:head
    [:title "Bid Pitch"]
    (include-css "/css/styles.css")
    (include-js "/js/lib/react-0.8.0.js" "/js/bin/main.js")
    (meta-viewport)]
   [:body
    [:div#content]]))

(defn page-game-create []
  (html5
   [:head
    [:title "Bid Pitch - Create Game"]
    (include-css "/css/styles.css")
    (meta-viewport)]
   [:body.page.create
    [:div.row1
     [:h1 "Create game"]]
    [:form {:method "POST" :action "/games/"}
     [:div.row2
      [:label {:for "title"} "What do you want to call your game?"]
      [:br]
      [:input {:type "text" :name "title"}]]
     [:div.row3
      [:input {:type "submit"}]]]]))

(defn page-game-join [games]
  (html5
   [:head
    [:title "Bid Pitch - Join Game"]
    (include-css "/css/styles.css")
    (meta-viewport)]
   [:body.page
    [:div.row1
     [:h1 "Join game"]]
    [:div.row2
     (vec (cons :ul (map (fn [game]
                           [:li (link-to (str (:id game)) (escape-html (:name game)))])
                         games)))]]))

(defn page-login []
  (html5
   [:head
    [:title "Login"]
    (meta-viewport)]
   [:body
    [:form {:method "POST" :action "login"}
     [:div "Username" [:input {:type "text" :name "username"}]]
     [:div "Password" [:input {:type "password" :name "password"}]]
     [:div [:input {:type "submit" :class "button" :value "Login"}]]]]))
