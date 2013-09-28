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

(defn insert-user [user]
  (insert users
          (values user))
  )

(defn find-user [username]
  (first
   (exec
    (->
     (select* users)
     (where {:username username})
     )))
  )
