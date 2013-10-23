(ns burgerboard.database
  (:use korma.db
        korma.core)
  (:require [clojure.java.jdbc :as jdbc]
            [crypto.password.bcrypt :as password])
  )

(defn assoc-id [entity insert-result]
  (assoc entity
    :id ((keyword "last_insert_rowid()")
         insert-result))
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
  (jdbc/create-table
   :stores
   [:id "INTEGER" "PRIMARY KEY"]
   [:board_id "INTEGER"]
   [:name "VARCHAR"]
   )
  (jdbc/create-table
   :ratings
   [:id "INTEGER" "PRIMARY KEY"]
   [:store_id "INTEGER"]
   [:user_email "VARCHAR"]
   [:rating "INTEGER"]
   )
  )

(declare users boards stores ratings)

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
  (belongs-to groups {:fk :group_id})
  (has-many stores)
  )

(defentity stores
  (entity-fields :name)
  (has-many ratings {:fk :store_id})
  (belongs-to boards {:fk :board_id})
  )

(defentity ratings
  (entity-fields :rating)
  (belongs-to stores {:fk :store_id})
  (belongs-to users {:fk :user_id})
  )

(defn insert-group [group]
  (insert groups
          (values group))
  )

(defn insert-member [group user]
  (transaction
   (let [stores (select stores
                        (where {:board_id [in (subselect
                                               boards
                                               (fields :id)
                                               (where {:group_id
                                                       (:id group)}))]})
                        )]
     (insert memberships
             (values {:user_email (:email user)
                      :group_id (:id group)}))
     (if (not-empty stores)
       (insert
        ratings
        (values (map (fn [store] {:user_email (:email user)
                                  :store_id (:id store)
                                  :rating nil})
                     stores)))
       )
    )
   )
  )

(defn insert-user [user]
  (let [groups (:groups user)
        user (dissoc user :groups)]
    (insert users
            (values user))
    (doseq [group groups]
      (insert-member group user)
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
  (assoc-id
   board
   (insert boards
           (values {:name (:name board)
                    :group_id (:id (:group board))}))
   )
  )

(defn find-users-boards [user]
  (map
   (fn [splated]
     {:id (:id splated)
      :name (:name splated)
      :group {:id (:group_id splated)
              :name (:group_name splated)}}
     )
   (select boards
           (fields :id :name)
           (with groups
                 (fields [:id :group_id] [:name :group_name]))
           (where (in :group_id (map :id (:groups user)))))
   )
  )

(defn insert-store [store]
  (transaction
   (let [inserted-store 
         (assoc-id
          store
          (insert stores
                  (values {:name (:name store)
                           :board_id (:id (:board store))}))
          )
         
         members
         (select memberships
                 (where {:group_id (:group_id (:board store))}))]
     ;; TODO: assoc the new ratings into the new store (but we don't
     ;; know all their IDs without re-querying)
     (if (not (empty? members))
       (insert
        ratings
        (values (map (fn [membership] {:user_email (:user_email membership)
                                       :store_id (:id inserted-store)
                                       :rating nil})
                     members))
        )
       )
     inserted-store
     )
   )
  )