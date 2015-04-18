(ns socky.cljs-macros
  (:require [om.core :as om]))

(defmacro om-render [body]
  `(reify
     om/IRender
     (~'render [~'_] ~body)))

