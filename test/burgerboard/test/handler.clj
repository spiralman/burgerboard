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
  (testing "login route"
    (testing "valid email and password"
      (let [response
            (app
             (->
              (request :post "/api/v1/login")
              (header :content-type "application/json")
              (body (json/write-str {:email "some_user@example.com"
                                     :password "password"}))
              ))]
        (is (= (:status response) 200))
        (is (contains? (:headers response) "Set-Cookie"))
        (is (= (json/read-str (:body response) :key-fn keyword)
               {:email "some_user@example.com"
                :name "Some User"
                :groups_url "http://localhost/api/v1/groups"
                :groups [{:name "Group" :id 1}]}))
        )
      )

    (testing "invalid email"
      (let [response
            (app
             (->
              (request :post "/api/v1/login")
              (body (json/write-str {:email "bad_user@example.com"
                                     :password "password"}))
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
              (body (json/write-str {:email "some_user@example.com"
                                     :password "wrong_pass"}))
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
              (body (json/write-str {:email "new_user@example.com"
                                     :password "password"
                                     :name "New User"}))
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
                  (body (json/write-str {:email "bad email"
                                         :password "password"
                                         :name "Bad user"}))
                  ))]
        (is (= (:status response) 400))
        )
      )
    (testing "Duplicate email"
      (let [response
            (app (->
                  (request :post "/api/v1/signups")
                  (body (json/write-str {:email "some_user@example.com"
                                         :password "password"
                                         :name "Duplicate user"}))
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
              (body (json/write-str {:email "second_user@example.com"
                                     :name "Second User"}))
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
              (body (json/write-str {:email "second_user@example.com"
                                     :name "Second User"}))
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
              (body (json/write-str {:email "second_user@example.com"
                                     :name "Second User"}))
              ))]
        (is (= (:status response) 201))
        (is (= {:email "second_user@example.com" :name "Second User"}
               (find-member (find-group 1) "second_user@example.com")))
        )
      )
    )

  (testing "groups route"
    (testing "requires login"
      (let [response
            (app
             (->
              (request :get "/api/v1/groups")
              (header :content-type "application/json")))]
        (is (= (:status response) 401))
        )
      )

    (testing "Returns just user's groups"
      (insert-group {:name "Group2" :owner "owner@example.com"})
      (insert-member {:id 2} {:email "owner@example.com"})
      (insert-member {:id 2} {:email "some_user@example.com"})

      (insert-group {:name "Other Group" :owner "other-owner@example.com"})

      (let [response
            (app
             (->
              (request :get "/api/v1/groups")
              (header "Cookie" (login-as "owner@example.com" "password"))))]
        (is (= (:status response) 200))
        (is (= {:groups [{:id 1
                          :name "Group"
                          :boards_url "http://localhost/api/v1/groups/1/boards"
                          :members_url "http://localhost/api/v1/groups/1/members"}
                         {:id 2
                          :name "Group2"
                          :boards_url "http://localhost/api/v1/groups/2/boards"
                          :members_url "http://localhost/api/v1/groups/2/members"}]}
               (json/read-str (:body response) :key-fn keyword)))
        )
      )
    )

  (testing "not-found route"
    (let [response (app (request :get "/invalid"))]
      (is (= (:status response) 404)))
    )
  )
