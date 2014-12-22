(ns burgerboard.test.database-test
  (:use clojure.test
        burgerboard.database
        korma.db
        korma.core)
  (:require
   [clojure.java.jdbc :as jdbc]
   )
  )

(defn get-table [name tables]
  (first (filter (fn [table] (= name (:table_name table))) tables)))

(defn get-column [name columns]
  (first (filter (fn [column] (= name (:column_name column))) columns)))

(defn get-tables []
  (jdbc/resultset-seq
   (-> (jdbc/connection)
       (.getMetaData)
       (.getTables nil nil nil (into-array ["TABLE"]))))
  )

(defn get-columns [table]
  (jdbc/resultset-seq
   (-> (jdbc/connection)
       (.getMetaData)
       (.getColumns nil nil table nil)))
  )

(def testing-db-spec
  (sqlite3
   {:db ":memory:"
    :make-pool true}))

(defdb testing-db
  testing-db-spec)

(defn with-database [f]
  (jdbc/with-connection testing-db-spec
    (create-schema)
    (f)
    (map (fn [table] (jdbc/drop-table (:table_name table))) (get-tables))
    )
  )

(use-fixtures :each with-database)

(deftest test-create-schema
  (testing "DB schema is created"
    (let [tables (get-tables)]
      (is (not (nil? (get-table "users" tables))))

      (let [users-cols (get-columns "users")]
        (is (= "VARCHAR" (:type_name (get-column "email" users-cols))))
        (is (= "VARCHAR" (:type_name (get-column "password" users-cols))))
        (is (= "VARCHAR" (:type_name (get-column "name" users-cols))))
        )

      (is (not (nil? (get-table "groups" tables))))

      (let [groups-cols (get-columns "groups")]
        (is (= "INTEGER" (:type_name (get-column "id" groups-cols))))
        (is (= "VARCHAR" (:type_name (get-column "owner" groups-cols))))
        (is (= "VARCHAR" (:type_name (get-column "name" groups-cols))))
        )

      (is (not (nil? (get-table "memberships" tables))))

      (let [membership-cols (get-columns "memberships")]
        (is (= "VARCHAR" (:type_name (get-column "user_email" membership-cols))))
        (is (= "INTEGER" (:type_name (get-column "group_id" membership-cols))))
        )

      (is (not (nil? (get-table "boards" tables))))

      (let [boards-cols (get-columns "boards")]
        (is (= "INTEGER" (:type_name (get-column "id" boards-cols))))
        (is (= "INTEGER" (:type_name (get-column "group_id" boards-cols))))
        (is (= "VARCHAR" (:type_name (get-column "name" boards-cols))))
        )

      (is (not (nil? (get-table "stores" tables))))

      (let [stores-cols (get-columns "stores")]
        (is (= "INTEGER" (:type_name (get-column "id" stores-cols))))
        (is (= "INTEGER" (:type_name (get-column "board_id" stores-cols))))
        (is (= "VARCHAR" (:type_name (get-column "name" stores-cols))))
        )

      (is (not (nil? (get-table "ratings" tables))))

      (let [ratings-cols (get-columns "ratings")]
        (is (= "INTEGER" (:type_name (get-column "id" ratings-cols))))
        (is (= "INTEGER" (:type_name (get-column "store_id" ratings-cols))))
        (is (= "VARCHAR" (:type_name (get-column "user_email" ratings-cols))))
        (is (= "INTEGER" (:type_name (get-column "rating" ratings-cols))))
        )
      )
    )

  (testing "create-schema is idempotent"
    (create-schema)
    )
  )

(deftest test-nest-subentity
  (testing "Nesting a sub-entity by prefix name"
    (is (= {:key "value" :nested {:key "value" :other "something"}}
           (nest-subentity
            :nested
            {:key "value" :nested_key "value" :nested_other "something"})
           ))
    (is (nil? (nest-subentity :foo nil)))
    )
  )

(deftest test-user-tables
  (testing "User insertion and retrieval"
    (insert-user {:email "user@example.com" :password "pass" :name "Name"})

    (is (= {:email "user@example.com" :password "pass" :name "Name"}
           (first (exec (select* users)))))

    (is (= {:email "user@example.com" :password "pass" :name "Name" :groups []}
           (find-user "user@example.com")))
    (is (= nil (find-user "asdf")))
    )
  )

(deftest test-groups
  (testing "User belonging to a group"
    (let [group (insert-group {:name "group" :owner "user"})]
      (is (= 1 (:id group)))
      (insert-user {:email "user@example.com" :password "pass" :name "Name"
                    :groups [{:id 1 :name "group"}]})

      (is (= {:email "user@example.com" :password "pass" :name "Name"
              :groups [{:id 1 :name "group"}]}
             (find-user "user@example.com")))

      (is (= {:id 1 :name "group" :owner "user"
              :users [{:email "user@example.com" :name "Name"}]}
             (find-group 1)))
      )
    )

  (testing "Adding a user to a group"
    (insert-user {:email "other@example.com" :password "pass" :name "Other"
                  :groups []})

    (insert-member {:id 1} {:email "other@example.com"})

    (is (= [{:id 1 :name "group"}] (:groups (find-user "other@example.com"))))
    (is (= {:email "other@example.com" :name "Other"}
           (first (filter
                   (fn [user] (= (:email user) "other@example.com"))
                   (:users (find-group 1))))))
    )

  (testing "Finding groups to which a member belongs"
    (insert-group {:name "group2" :owner "user@example.com"})
    (insert-member {:id 2} {:email "user@example.com"})

    (insert-group {:name "other group" :owner "other@example.com"})

    (is (= [{:id 1 :name "group"} {:id 2 :name "group2"}]
           (find-users-groups {:email "user@example.com"})))
    )
  )

(deftest test-boards
  (testing "Adding a board to a group"
    (insert-group {:name "group" :owner "user@example.com"})
    (insert-user {:email "user@example.com" :password "pass" :name "Name"
                  :groups [{:id 1 :name "group"}]})

    (let [new-board (insert-board {:name "board" :group {:id 1}})]
      (is (= {:id 1 :name "board" :group {:id 1}} new-board))
      (is (= {:id 1 :name "board" :group_id 1}
             (first (select boards))))
      )
    )

  (testing "Getting the boards for a group"
    (insert-board {:name "board2" :group {:id 1}})
    (insert-group {:name "group2" :owner "user@example.com"})

    (insert-board {:name "hidden" :group {:id 2}})

    (is (= [{:id 1
             :name "board"
             :group {:id 1}}
            {:id 2
             :name "board2"
             :group {:id 1}}]
           (find-groups-boards {:id 1})))
    )

  (testing "Getting a board by ID"
    (insert-group {:name "group2" :owner "user@example.com"})
    (insert-board {:name "board2" :group {:id 2}})

    (is (= {:id 1 :name "board" :group {:id 1 :name "group"}}
           (find-group-board {:id 1} 1)))

    (is (nil? (find-group-board {:id 2} 1)))
    )
  )

(deftest test-stores
  (testing "Creating a new store"
    (insert-group {:name "group" :owner "user@example.com"})
    (insert-user {:email "user@example.com" :password "pass" :name "Name"
                  :groups [{:id 1 :name "group"}]})
    (insert-user {:email "other@example.com" :password "pass" :name "Other"
                  :groups [{:id 1 :name "group"}]})
    (insert-board {:name "board" :group {:id 1}})

    (let [new-store (insert-store {:name "store" :board {:id 1 :group {:id 1}}})]
      (is (= {:id 1 :name "store" :board {:id 1 :group {:id 1}}} new-store))
      (is (= {:id 1 :name "store" :board_id 1}
             (first (select stores))))

      (is (= [{:id 1 :store_id 1 :user_email "user@example.com" :rating nil}
              {:id 2 :store_id 1 :user_email "other@example.com" :rating nil}]
             (select ratings (where {:store_id (:id new-store)}))))
      )
    )

  (testing "Finding a store"
    (insert-store {:name "other store" :board {:id 2 :group {:id 1}}})

    (is (= {:id 1 :name "store" :board {:id 1 :group_id 1}}
           (find-board-store {:id 1 :group_id 1} 1)))

    (is (nil? (find-board-store {:id 1 :group_id 1} 2)))
    )
  )

(defn user-has-rating [email store rating-value]
  (let [rating-record (first (select ratings
                       (where {:store_id (:id (first
                                               (select stores
                                                       (where {:name store}))))
                               :user_email email})))]

    (is (not (nil? rating-record)))
    (is (=
         (:rating rating-record)
         rating-value
         ))
    )
  )

(deftest test-new-user-ratings
  (testing "Adding users creates ratings"
    (insert-group {:name "group" :owner "user@example.com"})
    (insert-group {:name "other" :owner "user@example.com"})

    (insert-user {:email "other@example.com" :password "pass" :name "Other"
                  :groups []})

    (insert-board {:name "board" :group {:id 1}})
    (insert-board {:name "board2" :group {:id 1}})
    (insert-board {:name "other board" :group {:id 2}})

    (insert-store {:name "store" :board {:id 1 :group {:id 1}}})
    (insert-store {:name "store2" :board {:id 1 :group {:id 1}}})

    (insert-store {:name "other board store" :board {:id 2 :group {:id 1}}})

    (insert-store {:name "other group store" :board {:id 3 :group {:id 2}}})


    (insert-user {:email "user@example.com" :password "pass" :name "Name"
                  :groups [{:id 1 :name "group"} {:id 2 :name "other"}]})

    (insert-member {:id 1} {:email "other@example.com"})

    (user-has-rating "user@example.com" "store" nil)
    (user-has-rating "user@example.com" "store2" nil)
    (user-has-rating "user@example.com" "other board store" nil)
    (user-has-rating "user@example.com" "other group store" nil)

    (user-has-rating "other@example.com" "store" nil)
    (user-has-rating "other@example.com" "store2" nil)
    (user-has-rating "other@example.com" "other board store" nil)
    )
  )

(deftest test-ratings
  (testing "Setting ratings"
    (insert-group {:name "group" :owner "user@example.com"})
    (insert-user {:email "user@example.com" :password "pass" :name "Name"
                  :groups [{:id 1 :name "group"}]})
    (insert-user {:email "other@example.com" :password "pass" :name "Other"
                  :groups [{:id 1 :name "group"}]})
    (insert-board {:name "board" :group {:id 1}})
    (insert-store {:name "store" :board {:id 1 :group {:id 1}}})

    (is (= {:id 1 :name "store"
            :ratings [{:user_email "user@example.com"
                       :rating 1}
                      {:user_email "other@example.com"
                       :rating nil}]}
           (set-rating {:id 1} {:email "user@example.com"} 1)))
    (user-has-rating "user@example.com" "store" 1)
    (user-has-rating "other@example.com" "store" nil)

    (set-rating {:id 1} {:email "other@example.com"} 0)
    (user-has-rating "other@example.com" "store" 0)
    )

  (testing "Finding ratings for a board"
    (insert-user {:email "third@example.com" :password "pass" :name "Third"
                  :groups [{:id 1 :name "group"}]})

    (is (= [{:id 1 :name "store"
             :ratings [{:user_email "user@example.com"
                        :rating 1}
                       {:user_email "other@example.com"
                        :rating 0}
                       {:user_email "third@example.com"
                        :rating nil}]}]
           (find-board-ratings {:id 1})))
    )
  )
