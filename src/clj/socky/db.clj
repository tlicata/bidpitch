(ns socky.db
  (:require [cemerick.friend.credentials :as creds]
            [clojure.java.jdbc :as jdbc]))

(def pg-db "postgresql://localhost:5432/bidpitch_dev")
(def games-table "games")

(defn game-add [title]
  (jdbc/insert! pg-db :games {:name title}))
(defn game-all []
  (jdbc/query pg-db [(str "SELECT * FROM " games-table)]))

(defn player-query []
  (jdbc/query pg-db ["SELECT * FROM players"]))
(defn player-get [name]
  (jdbc/query pg-db ["SELECT * FROM players WHERE username = ?" name]
              :result-set-fn first))
(defn player-add [username pswd]
  (let [password (creds/hash-bcrypt pswd)]
    (jdbc/insert! pg-db :players {:username username
                                  :password password})))
(defn player-remove [name]
  (jdbc/delete! pg-db :players ["username = ?" name]))
