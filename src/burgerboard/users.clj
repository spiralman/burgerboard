(ns burgerboard.users
  (:import [javax.mail.internet InternetAddress AddressException])
  (:require [crypto.password.bcrypt :as password]))

(defn create-user [email password name]
  (try
    (.validate (InternetAddress. email))
    {:email email
     :password (password/encrypt password)
     :name name}
    (catch AddressException _)
    )
  )

(defn login-valid [user email password]
  (if user
    (and (= email (:email user))
         (password/check password (:password user))
         )
    )
  )

(defn is-owner? [group user]
  (= (:owner group) (:email user))
  )

(defn is-member? [group user]
  (some (fn [member] (= (:email user) (:email member))) (:users group))
  )
