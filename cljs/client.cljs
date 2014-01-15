(ns sockey.client
  (:require [dommy.core :as d])
  (:require-macros [dommy.macros :refer [node sel1]]))

(defn render-page []
  (node
   (list
    [:div
     [:h3 "Test"]
    [:div
     [:h3 "ing"]]])))

(set! (.-onload js/window)
      #(d/replace-contents! (sel1 :#content)
                            (render-page)))
