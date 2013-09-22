(ns burgerboard.test.handler
  (:use clojure.test
        [clojure.data.json :as json]
        ring.mock.request
        burgerboard.handler))

(deftest test-app
  (testing "main route"
    (let [response (app (request :get "/"))]
      (is (= (:status response) 200))
      (is (= (:body response) "Hello World"))))

  (testing "login route"
    (let [response
          (app
           (->
            (request :post "/api/v1/login")
            (content-type "application/json")
            (body (json/write-str {:foo "bar"}))
            ))]
      (is (= (:status response) 200))
      (is (contains? (:headers response) "Set-Cookie"))
      )
    )
  
  (testing "not-found route"
    (let [response (app (request :get "/invalid"))]
      (is (= (:status response) 404)))))
