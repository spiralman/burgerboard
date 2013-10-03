(ns burgerboard.test.board-handlers-test
  (:use clojure.test
        [clojure.data.json :as json]
        ring.mock.request
        burgerboard.test.test-fixtures
        burgerboard.handler)
  )

(use-fixtures :each single-user-fixture)

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
      )

    (testing "POST"
      (testing "requires ownership"
        (let [response
              (app
               (->
                (request :post "/api/v1/boards")
                (header :content-type "application/json")
                (body (json/write-str {:group 1
                                       :name "New Board"}))))]
          (is (= (:status response) 401))
          )
        )
      )
    )
  )
