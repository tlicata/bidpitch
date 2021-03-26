(ns bidpitch.cljs-macros)

(defmacro defview [name body]
  `(defn ~name []
     (let [~'data ~'@game-state]
       ~body)))

(defmacro safely [body]
  `(try ~body (catch :default _ nil)))
