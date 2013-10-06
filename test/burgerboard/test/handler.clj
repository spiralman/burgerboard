(ns burgerboard.test.handler
  (:use clojure.test
        [clojure.data.json :as json :only [read-str write-str]]
        ring.mock.request
        burgerboard.test.test-fixtures
        burgerboard.handler
        burgerboard.database
        burgerboard.users
        korma.db)
  (:require
   [clojure.java.jdbc :as jdbc])
  (:import [java.net HttpCookie])
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

  (testing "Signup route"
    (testing "Valid email"
      (let [response
            (app
             (->
              (request :post "/api/v1/signups")
              (body {:email "new_user@example.com"
                     :password "password"
                     :name "New User"})
              ))]
        (is (= (:status response) 201))
        (is (contains? (:headers response) "Set-Cookie"))
        (is (= {:email "new_user@example.com" :name "New User"}
               (json/read-str (:body response) :key-fn keyword)))
        (is (not (nil? (find-user "new_user@example.com"))))
        )
      )
    (testing "Invalid email"
      (let [response
            (app (->
                  (request :post "/api/v1/signups")
                  (body {:email "bad email"
                         :password "password"
                         :name "Bad user"})
                  ))]
        (is (= (:status response) 400))
        )
      )
    (testing "Duplicate email"
      (let [response
            (app (->
                  (request :post "/api/v1/signups")
                  (body {:email "some_user@example.com"
                         :password "password"
                         :name "Duplicate user"})
                  ))]
        (is (= (:status response) 400))
        )
      )
    )

  (testing "Invitation route"
    (testing "requires login"
      (let [response
            (app
             (->
              (request :post "/api/v1/groups/1/members")
              (body {:email "second_user@example.com"
                     :name "Second User"})
              ))]
        (is (= (:status response) 401))
        )
      )

    (testing "requires ownership"
      (let [response
            (app
             (->
              (request :post "/api/v1/groups/1/members")
              (header "Cookie" (login-as "some_user@example.com" "password"))
              (body {:email "second_user@example.com"
                     :name "Second User"})
              ))]
        (is (= (:status response) 403))
        )
      )

    (testing "inserts existing user"
      (let [response
            (app
             (->
              (request :post "/api/v1/groups/1/members")
              (header "Cookie" (login-as "owner@example.com" "password"))
              (body {:email "second_user@example.com"
                     :name "Second User"})
              ))]
        (is (= (:status response) 201))
        (is (= {:email "second_user@example.com" :name "Second User"}
               (find-member (find-group 1) "second_user@example.com")))
        )
      )
    )
  
  (testing "not-found route"
    (let [response (app (request :get "/invalid"))]
      (is (= (:status response) 404)))))
