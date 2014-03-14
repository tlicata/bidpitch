(ns socky.client
  (:require [chord.client :refer [ws-ch]]
            [cljs.core.async :refer [<! >! chan put!]]
            [cljs.reader :refer [read-string]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [socky.game :as game])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(def websocket-url "ws://localhost:8080/socky")
(def websocket (atom (chan)))

(def state (atom game/empty-state))

(defn send-message [msg]
  (put! @websocket (or msg "bid:pass")))

(defn app [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil (prn-str data)))))

(set! (.-onload js/window)
      (fn []
        (let [target (.getElementById js/document "content")]
          (go
           (reset! websocket (<! (ws-ch websocket-url)))
           (om/root state app target)
           (send-message "state")
           (loop []
             (when-let [msg (<! @websocket)]
               (reset! state (read-string (:message msg)))
               (recur)))))))
