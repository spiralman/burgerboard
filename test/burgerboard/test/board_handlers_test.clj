(ns burgerboard.test.board-handlers-test
  (:use clojure.test
        [clojure.data.json :as json :only [read-str write-str]]
        ring.mock.request
        burgerboard.test.test-fixtures
        burgerboard.handler)
  )

(use-fixtures :each basic-board-fixture)

(deftest test-board-handlers
  (testing "Boards route"
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
                    :group {:id 1 :name "Group"}}]
                  }
                 (json/read-str (:body response)
                                :key-fn keyword)))
          )
        )
      )

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
          )
        )
      )
    )
  )
