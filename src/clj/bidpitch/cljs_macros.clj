(ns bidpitch.cljs-macros
  (:require [om.core :as om]))

(defmacro defview [name body]
  `(defn ~name [~'data ~'owner]
     (reify
       om/IRender
       (~'render [~'_] ~body))))

(defmacro safely [body]
  `(try ~body (catch :default _ nil)))
