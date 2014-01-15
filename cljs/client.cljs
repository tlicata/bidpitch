(ns socky.client
  (:require [chord.client :refer [ws-ch]]
            [cljs.core.async :refer [<! >! chan]]
            [dommy.core :as d])
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [dommy.macros :refer [node sel1]]))

(defn render-page []
  (node
   (list
    [:div
     [:h3 "Test"]
    [:div
     [:h3 "ing"]]])))

(def websocket-url "ws://localhost:8080/socky")
(def websocket (atom (chan)))

(defn send-message [msg]
  (go
   (>! @websocket (or msg "pizza"))))

(set! (.-onload js/window)
      (fn []
        (go
         (reset! websocket (<! (ws-ch websocket-url)))
         (loop []
           (when-let [msg (<! @websocket)]
             (.log js/console (:message msg))
             (recur))))))
