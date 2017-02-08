(ns bidpitch.util)

;; like `map` but will continue through the end of the longest
;; sequence, instead of stopped with the shortest.
;; http://stackoverflow.com/questions/15769530/whats-the-idiomatic-way-to-map-vector-according-to-the-longest-seq-in-clojure
(defn map-all [f & colls]
  (lazy-seq
   (when (some seq colls)
     (cons (apply f (map first colls))
           (apply map-all f (map rest colls))))))
