(ns socky.db
  (:require [clojure.java.jdbc :as jdbc]))

(def pg-db {:subprotocol "postgresql"
            :subname "//localhost:5432/mydb"})

(def describe "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public';")
(def more "select * from information_schema.tables;")
