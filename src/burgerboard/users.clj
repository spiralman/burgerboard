(ns burgerboard.users
  (:require [crypto.password.bcrypt :as password]))

(defn create-user [email password]
  {:email email
   :password (password/encrypt password)}
  )

(defn login-valid [user email password]
  (if user
    (and (= email (:email user))
         (password/check password (:password user))
         )
    )
  )
