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
  (jdbc/create-table
   :groups
   [:id "INTEGER" "PRIMARY KEY"]
   [:name "varchar"]
   [:owner "varchar"]
   )
  (jdbc/create-table
   :memberships
   [:id "INTEGER" "PRIMARY KEY"]
   [:user_username "VARCHAR"]
   [:group_id "INTEGER"]
   )
  )

(declare users)

(defentity groups
  (entity-fields :name)
  (has-one users)
  (many-to-many users :memberships {:lfk :group_id :rfk :user_username})
  )

(defentity users
  (pk :username)
  (entity-fields :password)
  (many-to-many groups :memberships {:lfk :user_username :rfk :group_id})
  )

(defentity memberships
  ;; Only used internally for inserting the relationships
  (entity-fields :user_username :group_id)
  )

(defn insert-group [group]
  (insert groups
          (values group))
  )

(defn insert-user [user]
  (let [groups (:groups user)
        user (dissoc user :groups)]
    (insert users
            (values user))
    (if groups
      (insert memberships
              (values (map
                       (fn [group]
                         {:user_username (:username user)
                          :group_id (:id group)})
                       groups))
              )
      )
    )
  )

(defn find-user [username]
  (first
   (select
    users
    (with groups
          (fields :id :name))
    (where {:username username})
    ))
  )

(defn find-group [id]
  (first
   (select
    groups
    (with users
          (fields :username))
    (where {:id id})
    ))
  )
          
