(ns burgerboard.test.test-fixtures
  (:use korma.db
        ring.mock.request
        burgerboard.database
        burgerboard.users
        burgerboard.handler
        )
  (:require
   [clojure.java.jdbc :as jdbc])
  (:import [java.net HttpCookie])
  )

(defn login-as [email password]
  (->
   (app
    (->
     (request :post "/api/v1/login")
     (body {:email email
            :password password})))
   (:headers)
   (get "Set-Cookie")
   (first)
   (HttpCookie/parse)
   (first)
   (.toString)
   )
  )

(defn find-member [groups email]
  (first
   (filter
    (fn [user]
      (= (:email user) email))
    (:users groups)))
  )

(def testing-db-spec
  (sqlite3
   {:db ":memory:"
    :make-pool true}))

(defdb testing-db
  testing-db-spec)

(defn single-user-fixture [f]
  (jdbc/with-connection testing-db-spec
    (create-schema)
    (insert-user (create-user "owner@example.com" "password" "Owner User"))
    (insert-user (create-user "some_user@example.com" "password" "Some User"))
    (insert-user (create-user "second_user@example.com" "password" "Second User"))
    (insert-group {:name "Group" :owner "owner@example.com"})
    (insert-member {:id 1} {:email "owner@example.com"})
    (insert-member {:id 1} {:email "some_user@example.com"})
    (f)
    )
  )

(defn single-board-fixture [f]
  (single-user-fixture
   (fn []
     (insert-board {:name "Some Board" :group 1})
     (f)
     )
   )
  )
