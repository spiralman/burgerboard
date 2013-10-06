(ns burgerboard.database
  (:use korma.db
        korma.core)
  (:require [clojure.java.jdbc :as jdbc]
            [crypto.password.bcrypt :as password])
  )

(defn create-schema []
  (jdbc/create-table
   :users
   [:email "varchar" "PRIMARY KEY"]
   [:password "varchar"]
   [:name "varchar"]
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
   [:user_email "VARCHAR"]
   [:group_id "INTEGER"]
   )
  (jdbc/create-table
   :boards
   [:id "INTEGER" "PRIMARY KEY"]
   [:group_id "INTEGER"]
   [:name "varchar"]
   )
  )

(declare users boards)

(defentity groups
  (entity-fields :name)
  (has-one users)
  (many-to-many users :memberships {:lfk :group_id :rfk :user_email})
  (has-many boards)
  )

(defentity users
  (pk :email)
  (entity-fields :password :name)
  (many-to-many groups :memberships {:lfk :user_email :rfk :group_id})
  )

(defentity memberships
  ;; Only used internally for inserting the relationships
  (entity-fields :user_email :group_id)
  )

(defentity boards
  (entity-fields :name)
  (belongs-to groups)
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
    (if (not-empty groups)
      (insert memberships
              (values (map
                       (fn [group]
                         {:user_email (:email user)
                          :group_id (:id group)})
                       groups))
              )
      )
    )
  )

(defn find-user [email]
  (first
   (select
    users
    (with groups
          (fields :id :name))
    (where {:email email})
    ))
  )

(defn insert-member [group user]
  (insert memberships
          (values {:user_email (:email user)
                   :group_id (:id group)}))
  )

(defn find-group [id]
  (first
   (select
    groups
    (with users
          (fields :email :name))
    (where {:id id})
    ))
  )

(defn insert-board [board]
  (insert boards
          (values {:name (:name board)
                   :group_id (:id (:group board))}))
  )