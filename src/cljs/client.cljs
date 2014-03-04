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

(defn send-message [msg]
  (put! @websocket (or msg "bid:pass")))

(defn chat [player message]
  (put! @websocket (str "chat:" player ":" message)))

(defn app [data owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (go-loop []
               (when-let [msg (<! @websocket)]
                 (.log js/console "message received" (:message msg))
                 (om/set-state! owner :message (:message msg))
                 (recur))))
    om/IRender
    (render [_]
      (dom/div nil
               (let [msg (om/get-state owner :message)]
                 (.log js/console "render")
                 (or msg (prn-str (-> game/empty-state
                                      (game/add-player "tim")
                                      (game/add-player "louise")
                                      (game/add-cards)
                                      (game/dealt-state "tim")))))))))

(set! (.-onload js/window)
      (fn []
        (let [target (.getElementById js/document "content")]
          (go
           (reset! websocket (<! (ws-ch websocket-url)))
           (om/root {} app target)))))
