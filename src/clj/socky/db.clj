(ns socky.db
  (:require [clojure.java.jdbc :as jdbc]))

(def pg-db "postgresql://localhost:5432/bidpitch_dev")
(def games-table "games")
(def get-query (str "SELECT * FROM " games-table " WHERE id = ?"))

(defn game-add []
  (first (jdbc/insert! pg-db :games {:name "untitled"})))
(defn game-get [id]
  (first (jdbc/query pg-db [get-query id])))
(defn game-all []
  (jdbc/query pg-db [(str "SELECT * FROM " games-table)]))
