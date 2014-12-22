(ns burgerboard.test.test-fixtures
  (:use [clojure.data.json :as json :only [read-str write-str]]
        korma.db
        korma.core
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
     (body (json/write-str {:email email
                            :password password}))))
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

(defn basic-board-fixture [f]
  (single-user-fixture
   (fn []
     (insert-board {:name "Some Board" :group {:id 1}})
     (insert-board {:name "Other Board" :group {:id 2}})

     (insert-store {:name "Store 1" :board {:id 1 :group {:id 1}}})
     (insert-store {:name "Store 2" :board {:id 1 :group {:id 1}}})

     (set-rating {:id 1} {:email "owner@example.com"} 1)
     (set-rating {:id 1} {:email "some_user@example.com"} 2)

     (set-rating {:id 2} {:email "owner@example.com"} 2)

     (insert-store {:name "Other Store" :board {:id 2 :group {:id 1}}})
     (f)
     )
   )
  )
