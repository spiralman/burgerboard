(ns burgerboard.database
  (:use korma.db)
  (:require [clojure.java.jdbc :as jdbc])
  )

(defn create-schema []
  (jdbc/create-table
   :users
   [:username "varchar(32)" "PRIMARY KEY"]
   )
  )