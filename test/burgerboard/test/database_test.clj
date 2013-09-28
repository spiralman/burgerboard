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
      )
    )
  )

(deftest test-user-tables
  (testing "User insertion and retrieval"
    (insert-user {:username "user" :password "pass"})

    (is (= {:username "user" :password "pass"} (first (exec (select* users)))))

    (is (= {:username "user" :password "pass"} (find-user "user")))
    (is (= nil (find-user "asdf")))
    )
  )

