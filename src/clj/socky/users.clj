(ns socky.users
  (:require [cemerick.friend.credentials :as creds]))

(def users {"tim" {:username "tim"
                   :password (creds/hash-bcrypt "tim_pass")
                   :roles #{::user}}
            "louise" {:username "louise"
                      :password (creds/hash-bcrypt "louise_pass")
                      :roles #{::user}}
            "sharon" {:username "sharon"
                      :password (creds/hash-bcrypt "sharon_pass")
                      :roles #{::user}}})
