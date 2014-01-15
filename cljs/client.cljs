(ns socky.client
  (:require [dommy.core :as d])
  (:require-macros [dommy.macros :refer [node sel1]]))

(defn render-page []
  (node
   (list
    [:div
     [:h3 "Test"]
    [:div
     [:h3 "ing"]]])))

(def websocket-url "ws://localhost:8080/socky")
(def websocket (js/WebSocket. websocket-url))

(set! (.-onmessage websocket)
      (fn [event]
        (let [message (.-data event)]
          (.log js/console message))))

(defn send-message [msg]
  (.send websocket (or msg "pizza")))

(set! (.-onload js/window)
      #(d/replace-contents! (sel1 :#content)
                            (render-page)))
