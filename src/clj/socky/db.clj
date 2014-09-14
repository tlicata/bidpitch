(ns socky.db
  (:require [clojure.java.jdbc :as jdbc]))

(def pg-db "postgresql://localhost:5432/bidpitch_dev")
(def games-table "games")

(def create-games-table-sql
  (str "create table " games-table " (
    id bigserial primary key,
    name varchar(255) not null
  );"))

(defn game-add [title]
  (jdbc/insert! pg-db :games {:name title}))
(defn game-all []
  (jdbc/query pg-db [(str "SELECT * FROM " games-table)]))

(defn migrated? []
  (-> (jdbc/query pg-db
                 [(str "select count(*) from information_schema.tables "
                       "where table_name='" games-table "'")])
      first :count pos?))
(defn migrate []
  (when (not (migrated?))
    (print "Creating database structure...") (flush)
    (jdbc/execute! pg-db [create-games-table-sql])
    (println " done")))
