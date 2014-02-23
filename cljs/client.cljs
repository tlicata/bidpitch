(ns socky.client
  (:require [chord.client :refer [ws-ch]]
            [cljs.core.async :refer [<! >! chan put!]]
            [cljs.reader :refer [read-string]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(def websocket-url "ws://localhost:8080/socky")
(def websocket (atom (chan)))

(defn send-message [msg]
  (put! @websocket (or msg "bid:pass")))

(defn chat [player message]
  (put! @websocket (str "chat:" player ":" message)))

(set! (.-onload js/window)
      (fn []
        (go
         (reset! websocket (<! (ws-ch websocket-url)))
         (om/root
          {}
          (fn [data owner]
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
                           (if msg
                             msg
                             "Hello World"))))))
         (.getElementById js/document "content"))
         (>! @websocket "state"))))
