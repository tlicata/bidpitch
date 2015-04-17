(ns socky.cljs-macros)

(defmacro om-render [body]
  (list 'reify
        'om/IRender
        (list 'render '[_] body)))
