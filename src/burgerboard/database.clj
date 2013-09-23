(ns burgerboard.database
  (:use korma.db
        korma.core)
  (:require [clojure.java.jdbc :as jdbc]
            [crypto.password.bcrypt :as password])
  )

(defn create-schema []
  (jdbc/create-table
   :users
   [:username "varchar" "PRIMARY KEY"]
   [:password "varchar"]
   )
  )

(defentity users
  (pk :username)
  (entity-fields :password)
  )

(defn create-user [username password]
  (insert users
          (values {:username username :password (password/encrypt password)})))

(defn login-valid [username password]
  (if-let [user (first
                 (exec
                  (->
                   (select* users)
                   (where {:username username})))
                 )]
    (password/check
     password
     (:password
      user
      )
     )
    )
  )