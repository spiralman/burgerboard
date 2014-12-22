(ns burgerboard.database
  (:use korma.db
        korma.core)
  (:require [clojure.java.jdbc :as jdbc]
            [crypto.password.bcrypt :as password]
            clojure.set)
  )

(defn assoc-id [entity insert-result]
  (let [last-insert (keyword "last_insert_rowid()")]
    (assoc entity
      :id
      (if (contains? insert-result last-insert)
        (last-insert insert-result)
        (:id insert-result))
      )
    ))

(defn prefixed? [prefix key]
  (.startsWith (name key) (name prefix))
  )

(defn remove-prefix [prefix key]
  (let [prefix-name (name prefix)
        key-name (name key)]
    (keyword
     (.substring key-name (+ (.length prefix-name) 1)))
    )
  )

(defn nest-subentity [prefix splated]
  (if splated
    (let [{member-keys false nested-keys true}
          (group-by
           (partial prefixed? prefix)
           (keys splated))]
      (->
       (select-keys splated member-keys)
       (assoc prefix (reduce-kv
                      (fn [nested key value]
                        (assoc nested
                          (remove-prefix prefix key) value)
                        )
                      {} (select-keys splated nested-keys)
                      ))
       )
      )
    )
  )

(defn create-schema []
  (jdbc/create-table
   "IF NOT EXISTS users"
   [:email "varchar" "PRIMARY KEY"]
   [:password "varchar"]
   [:name "varchar"]
   )
  (jdbc/create-table
   "IF NOT EXISTS groups"
   [:id "INTEGER" "PRIMARY KEY"]
   [:name "varchar"]
   [:owner "varchar"]
   )
  (jdbc/create-table
   "IF NOT EXISTS memberships"
   [:id "INTEGER" "PRIMARY KEY"]
   [:user_email "VARCHAR"]
   [:group_id "INTEGER"]
   )
  (jdbc/create-table
   "IF NOT EXISTS boards"
   [:id "INTEGER" "PRIMARY KEY"]
   [:group_id "INTEGER"]
   [:name "varchar"]
   )
  (jdbc/create-table
   "IF NOT EXISTS stores"
   [:id "INTEGER" "PRIMARY KEY"]
   [:board_id "INTEGER"]
   [:name "VARCHAR"]
   )
  (jdbc/create-table
   "IF NOT EXISTS ratings"
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
  (assoc-id
   group
   (insert groups
           (values group))
   )
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
     group
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

(defn find-users-groups [user]
  (select
   groups
   (fields :id :name)
   (where {:id [in (subselect
                    memberships
                    (fields :group_id)
                    (where {:user_email
                            (:email user)}))]}))
  )

(defn insert-board [board]
  (assoc-id
   board
   (insert boards
           (values {:name (:name board)
                    :group_id (:id (:group board))}))
   )
  )

(defn find-group-board [group board-id]
  (nest-subentity
   :group
   (first
    (select boards
            (fields :id :name)
            (with groups
                  (fields [:id :group_id] [:name :group_name]))
            (where {:id board-id :group_id (:id group)}))))
  )

(defn find-groups-boards [group]
  (map
   (fn [board]
     (assoc board :group group))
   (select boards
           (fields :id :name)
           (where {:group_id (:id group)})))
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
                 (where {:group_id (:id (:group (:board store)))}))]
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

(defn find-board-store [board store-id]
  (nest-subentity
   :board
   (first
    (select stores
            (fields :id :name)
            (with boards
                  (fields [:id :board_id] [:group_id :board_group_id])
            (where {:id store-id :board_id (:id board)})))
    )
   )
  )

(defn set-rating [store user rating]
  (transaction
   (update
    ratings
    (set-fields {:rating rating})
    (where {:store_id (:id store)
            :user_email (:email user)})
    )
   (first
    (select stores
            (fields :id :name)
            (with ratings
                  (fields :user_email :rating))
            (where {:id (:id store)}))
            )
   )
  )

(defn find-board-ratings [board]
  (select stores
          (fields :id :name)
          (with ratings
                (fields :user_email :rating))
          (where {:board_id (:id board)}))
  )
