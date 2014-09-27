(ns socky.db
  (:require [clojure.java.jdbc :as jdbc]))

(def pg-db "postgresql://localhost:5432/bidpitch_dev")
(def games-table "games")

(defn game-add [title]
  (jdbc/insert! pg-db :games {:name title}))
(defn game-all []
  (jdbc/query pg-db [(str "SELECT * FROM " games-table)]))
