(ns burgerboard.test.handler
  (:use clojure.test
        [clojure.data.json :as json]
        ring.mock.request
        burgerboard.handler
        burgerboard.database
        burgerboard.users
        korma.db)
    (:require
     [clojure.java.jdbc :as jdbc])
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
    (insert-user (create-user "some_user@example.com" "password"))
    (f)
    )
  )

(use-fixtures :each single-user-fixture)

(deftest test-app
  (testing "main route"
    (let [response (app (request :get "/"))]
      (is (= (:status response) 200))
      (is (= (:body response) "Hello World"))))

  (testing "login route"
    (testing "valid email and password"
      (let [response
            (app
             (->
              (request :post "/api/v1/login")
              (body {:email "some_user@example.com"
                     :password "password"})
              ))]
        (is (= (:status response) 200))
        (is (contains? (:headers response) "Set-Cookie"))
        )
      )

    (testing "invalid email"
      (let [response
            (app
             (->
              (request :post "/api/v1/login")
              (body {:email "bad_user@example.com"
                     :password "password"})
              ))]
        (is (= (:status response) 403))
        (is (not (contains? (:headers response) "Set-Cookie")))
        )
      )

    (testing "invalid password"
      (let [response
            (app
             (->
              (request :post "/api/v1/login")
              (body {:email "some_user@example.com"
                     :password "wrong_pass"})
              ))]
        (is (= (:status response) 403))
        (is (not (contains? (:headers response) "Set-Cookie")))
        )
      )
    )

  (testing "Boards route"
    (testing "requires login"
      (let [response
            (app
             (request :get "/api/v1/boards"))
            ]
        (is (= (:status response) 401))
        (is (= (:body response) "Login required"))
        )
      )
    )
  
  (testing "not-found route"
    (let [response (app (request :get "/invalid"))]
      (is (= (:status response) 404)))))
