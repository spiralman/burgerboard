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
        (is (= "VARCHAR" (:type_name (get-column "username" users-cols))))
        (is (= "VARCHAR" (:type_name (get-column "password" users-cols))))
        )

      (is (not (nil? (get-table "groups" tables))))

      (let [groups-cols (get-columns "groups")]
        (is (= "INTEGER" (:type_name (get-column "id" groups-cols))))
        (is (= "VARCHAR" (:type_name (get-column "owner" groups-cols))))
        (is (= "VARCHAR" (:type_name (get-column "name" groups-cols))))
        )

      (is (not (nil? (get-table "memberships" tables))))

      (let [membership-cols (get-columns "memberships")]
        (is (= "VARCHAR" (:type_name (get-column "user_username" membership-cols))))
        (is (= "INTEGER" (:type_name (get-column "group_id" membership-cols))))
        )
      )
    )
  )

(deftest test-user-tables
  (testing "User insertion and retrieval"
    (insert-user {:username "user" :password "pass"})

    (is (= {:username "user" :password "pass"} (first (exec (select* users)))))

    (is (= {:username "user" :password "pass" :groups []} (find-user "user")))
    (is (= nil (find-user "asdf")))
    )
  )

(deftest test-groups
  (testing "User belonging to a group"
    (insert-group {:name "group" :owner "user"})
    (insert-user {:username "user" :password "pass"
                  :groups [{:id 1 :name "group"}]})

    (is (= {:username "user" :password "pass"
            :groups [{:id 1 :name "group"}]}
           (find-user "user")))

    (is (= {:id 1 :name "group" :owner "user" :users [{:username "user"}]}
           (find-group 1)))
    )
  )

