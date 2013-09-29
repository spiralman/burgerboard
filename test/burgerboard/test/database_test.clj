(ns burgerboard.test.database-test
  (:use clojure.test
        burgerboard.database
        korma.db
        korma.core)
  (:require
   [clojure.java.jdbc :as jdbc]
   clojure.pprint))

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
      )
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
    (insert-group {:name "group" :owner "user"})
    (insert-user {:email "user@example.com" :password "pass" :name "Name"
                  :groups [{:id 1 :name "group"}]})

    (is (= {:email "user@example.com" :password "pass" :name "Name"
            :groups [{:id 1 :name "group"}]}
           (find-user "user@example.com")))

    (is (= {:id 1 :name "group" :owner "user"
            :users [{:email "user@example.com" :name "Name"}]}
           (find-group 1)))
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
  )

