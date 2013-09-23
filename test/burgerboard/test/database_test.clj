(ns burgerboard.test.database-test
  (:use clojure.test
        burgerboard.database
        korma.db)
  (:require
   [clojure.java.jdbc :as jdbc]
   clojure.pprint))

(defn get-table [name tables]
  (first (filter (fn [table] (= name (:table_name table))) tables)))

(def testing-db-spec
  (sqlite3
   {:db ":memory:"
    :make-pool true}))

(defdb testing-db
  testing-db-spec)

(deftest test-create-schema
  (testing "DB schema is created"
    (jdbc/with-connection testing-db-spec
      (create-schema)

      (let [tables 
            (jdbc/resultset-seq
             (-> (jdbc/connection)
                 (.getMetaData)
                 (.getTables nil nil nil (into-array ["TABLE" "VIEW"]))))]
        (is (not (nil? (get-table "users" tables))))
        )
      )
    )
  )

