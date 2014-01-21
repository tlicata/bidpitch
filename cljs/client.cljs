(ns socky.client
  (:require [chord.client :refer [ws-ch]]
            [cljs.core.async :refer [<! >! chan]]
            [cljs.reader :refer [read-string]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn widget [data owner]
  (om/component
   (dom/div nil "Hello world!")))

(def websocket-url "ws://localhost:8080/socky")
(def websocket (atom (chan)))

(defn send-message [msg]
  (go
   (>! @websocket (or msg "pizza"))))

(set! (.-onload js/window)
      (fn []
        (om/root {} widget (.getElementById js/document "content"))
        (go
         (reset! websocket (<! (ws-ch websocket-url)))
         (loop []
           (when-let [msg (<! @websocket)]
             (.log js/console (str (keys (read-string (:message msg)))))
             (recur))))))
