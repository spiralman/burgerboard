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
    (insert-user (create-user "some_user" "password"))
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
    (testing "valid username and password"
      (let [response
            (app
             (->
              (request :post "/api/v1/login")
              (content-type "application/json")
              (body {:username "some_user"
                     :password "password"})
              ))]
        (is (= (:status response) 200))
        (is (contains? (:headers response) "Set-Cookie"))
        )
      )

    (testing "invalid username"
      (let [response
            (app
             (->
              (request :post "/api/v1/login")
              (content-type "application/json")
              (body {:username "bad_user"
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
              (content-type "application/json")
              (body {:username "some_user"
                     :password "wrong_pass"})
              ))]
        (is (= (:status response) 403))
        (is (not (contains? (:headers response) "Set-Cookie")))
        )
      )
    )
  
  (testing "not-found route"
    (let [response (app (request :get "/invalid"))]
      (is (= (:status response) 404)))))
