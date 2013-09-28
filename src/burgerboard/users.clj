(ns burgerboard.users
  (:require [crypto.password.bcrypt :as password]))

(defn create-user [username password]
  {:username username
   :password (password/encrypt password)}
  )

(defn login-valid [user username password]
  (if user
    (and (= username (:username user))
         (password/check password (:password user))
         )
    )
  )
