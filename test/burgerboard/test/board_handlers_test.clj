(ns burgerboard.test.board-handlers-test
  (:use clojure.test
        [clojure.data.json :as json :only [read-str write-str]]
        ring.mock.request
        burgerboard.test.test-fixtures
        burgerboard.handler
        burgerboard.database
        korma.core)
  )

(use-fixtures :each basic-board-fixture)

(deftest test-board-handlers
  (testing "User's boards route"
    (testing "GET"
      (testing "requires login"
        (let [response
              (app
               (request :get "/api/v1/boards"))
              ]
          (is (= (:status response) 401))
          (is (= (:body response) "Login required"))
          )
        )

      (testing "returns boards in users groups"
        (let [response
              (app
               (->
                (request :get "/api/v1/boards")
                (header "Cookie" (login-as "some_user@example.com"
                                           "password"))))]
          (is (= (:status response) 200))
          (is (= {:boards
                  [{:name "Some Board"
                    :id 1
                    :url "http://localhost/api/v1/groups/1/boards/1"
                    :group {:id 1 :name "Group"}}]
                  }
                 (json/read-str (:body response)
                                :key-fn keyword)))
          )
        )
      )
    )

  (testing "Group's boards route"
    (testing "POST"
      (testing "requires login"
        (let [response
              (app
               (->
                (request :post "/api/v1/groups/1/boards")
                (header :content-type "application/json")
                (body (json/write-str {:name "New Board"}))))]
          (is (= (:status response) 401))
          )
        )

      (testing "requires ownership"
        (let [response
              (app
               (->
                (request :post "/api/v1/groups/1/boards")
                (header :content-type "application/json")
                (header "Cookie" (login-as "some_user@example.com"
                                           "password"))
                (body (json/write-str {:name "New Board"}))))]
          (is (= (:status response) 403))
          )
        )

      (testing "creates new board"
        (let [response
              (app
               (->
                (request :post "/api/v1/groups/1/boards")
                (header :content-type "application/json")
                (header "Cookie" (login-as "owner@example.com" "password"))
                (body (json/write-str {:name "New Board"}))))]
          (is (= (:status response) 201))
          (is (= {:name "New Board"
                  :id 3
                  :url "http://localhost/api/v1/groups/1/boards/3"
                  :group {:id 1 :name "Group"}}
                 (json/read-str (:body response)
                                :key-fn keyword)))
          )
        )
      )
    )

  (testing "Individual Board routes"
    (testing "GET"
      (testing "requires login"
        (let [response
              (app
               (->
                (request :get "/api/v1/groups/1/boards/1")
                (header :content-type "application/json")))]
          (is (= (:status response) 401))
          )
        )

      (testing "requires membership"
        (let [response
              (app
               (->
                (request :get "/api/v1/groups/1/boards/1")
                (header "Cookie" (login-as "second_user@example.com"
                                           "password"))))]
          (is (= (:status response) 403))
          )
        )

      (testing "requires board belongs to group"
        (insert-group {:name "group2" :owner "some_user@example.com"})
        (insert-member {:id 2} {:email "some_user@example.com"})
        
        (insert-board {:name "board2" :group {:id 2}})

        (let [response
              (app
               (->
                (request :get "/api/v1/groups/1/boards/2")
                (header "Cookie" (login-as "some_user@example.com"
                                           "password"))))]
          (is (= (:status response) 404))
          )
        )

      (testing "returns ratings"
        (let [response
              (app
               (->
                (request :get "/api/v1/groups/1/boards/1")
                (header "Cookie" (login-as "some_user@example.com"
                                           "password"))))]

          (is (= (:status response) 200))
          (is (= {:id 1 :name "Some Board"
                  :group {:name "Group" :id 1}
                  :url "http://localhost/api/v1/groups/1/boards/1"
                  :stores [{:name "Store 1"
                            :id 1
                            :rating_url
                            "http://localhost/api/v1/groups/1/boards/1/stores/1/rating"
                            :rating 1.5
                            :ratings [{:user_email "owner@example.com"
                                       :rating 1}
                                      {:user_email "some_user@example.com"
                                       :rating 2}]}
                           {:name "Store 2"
                            :id 2
                            :rating_url
                            "http://localhost/api/v1/groups/1/boards/1/stores/2/rating"
                            :rating 2.0
                            :ratings [{:user_email "owner@example.com"
                                       :rating 2}
                                      {:user_email "some_user@example.com"
                                       :rating nil}]
                            }]}
                 (json/read-str (:body response) :key-fn keyword)))
          )
        )
      )
    )

  (testing "Store routes"
    (testing "POST"
      (testing "requires login"
        (let [response
              (app
               (->
                (request :post "/api/v1/groups/1/boards/1/stores")
                (header :content-type "application/json")))]
          (is (= (:status response) 401))
          )
        )

      (testing "requires membership"
        (let [response
              (app
               (->
                (request :post "/api/v1/groups/1/boards/1/stores")
                (header "Cookie" (login-as "second_user@example.com"
                                           "password"))))]
          (is (= (:status response) 403))
          )
        )

      (testing "board must belong to group"
        (insert-group {:name "group2" :owner "some_user@example.com"})
        (insert-member {:id 2} {:email "some_user@example.com"})
        
        (insert-board {:name "board2" :group {:id 2}})

        (let [response
              (app
               (->
                (request :post "/api/v1/groups/1/boards/2/stores")
                (header "Cookie" (login-as "some_user@example.com"
                                           "password"))
                (body (json/write-str {:name "New Store"}))))]
          (is (= (:status response) 404))
          )
        )

      (testing "Adds new store"
        (let [response
              (app
               (->
                (request :post "/api/v1/groups/1/boards/1/stores")
                (header "Cookie" (login-as "some_user@example.com"
                                           "password"))
                (body (json/write-str {:name "New Store"}))))]
          (is (= (:status response) 201))
          (is (= {:name "New Store" :id 4
                  :rating_url
                  "http://localhost/api/v1/groups/1/boards/1/stores/4/rating"
                  :board {:id 1 :name "Some Board"
                          :group {:id 1 :name "Group"}}}
                 (json/read-str (:body response)
                                :key-fn keyword)))
          (is (= {:name "New Store" :id 4 :board_id 1}
                 (first (select
                         stores
                         (where {:id 4})))))
          )
        )
      )

    (testing "PUT to rating"
      (testing "requires login"
        (let [response
              (app
               (->
                (request :put "/api/v1/groups/1/boards/1/stores/1/rating")
                (header :content-type "application/json")))]
          (is (= (:status response) 401))
          )
        )

      (testing "requires membership"
        (let [response
              (app
               (->
                (request :put "/api/v1/groups/1/boards/1/stores/1/rating")
                (header "Cookie" (login-as "second_user@example.com"
                                           "password"))))]
          (is (= (:status response) 403))
          )
        )

      (testing "board must belong to group"
        (insert-group {:name "group2" :owner "some_user@example.com"})
        (insert-member {:id 2} {:email "some_user@example.com"})
        
        (insert-board {:name "board2" :group {:id 2}})

        (let [response
              (app
               (->
                (request :put "/api/v1/groups/1/boards/2/stores/1/rating")
                (header "Cookie" (login-as "some_user@example.com"
                                           "password"))
                (body (json/write-str {:rating 2}))))]
          (is (= (:status response) 404))
          )
        )

      (testing "store must belong to board"
        (let [response
              (app
               (->
                (request :put "/api/v1/groups/1/boards/1/stores/3/rating")
                (header "Cookie" (login-as "some_user@example.com"
                                           "password"))
                (body (json/write-str {:rating 2}))))]
          (is (= (:status response) 404))
          )
        )

      (testing "store must belong to board"
        (let [response
              (app
               (->
                (request :put "/api/v1/groups/1/boards/1/stores/1/rating")
                (header "Cookie" (login-as "some_user@example.com"
                                           "password"))
                (body (json/write-str {:rating 3}))))]
          (is (= (:status response) 200))
          (is (= {:name "Store 1"
                  :id 1
                  :rating_url
                  "http://localhost/api/v1/groups/1/boards/1/stores/1/rating"
                  :rating 2.0
                  :ratings [{:user_email "owner@example.com"
                             :rating 1}
                            {:user_email "some_user@example.com"
                             :rating 3}]}
                 (json/read-str (:body response) :key-fn keyword)))
          (is (= {:rating 3}
                 (first
                  (select ratings
                          (fields :rating)
                          (where {:store_id 1
                                  :user_email "some_user@example.com"})))))
          )
        )
      )
    )
  )