(ns burgerboard.test.handler
  (:use clojure.test
        [clojure.data.json :as json]
        ring.mock.request
        burgerboard.handler))

(def db {})

(deftest test-app
  (testing "main route"
    (let [response ((bind-app db) (request :get "/"))]
      (is (= (:status response) 200))
      (is (= (:body response) "Hello World"))))

  (testing "login route"
    (testing "valid username and password"
      (let [response
            ((bind-app db)
             (->
              (request :post "/api/v1/login")
              (content-type "application/json")
              (body (json/write-str {:username "some_user"
                                     :password "password"}))
              ))]
        (is (= (:status response) 200))
        (is (contains? (:headers response) "Set-Cookie"))
        )
      )

    (testing "invalid username"
      (let [response
            ((bind-app db)
             (->
              (request :post "/api/v1/login")
              (content-type "application/json")
              (body (json/write-str {:username "bad_user"
                                     :password "password"}))
              ))]
        (is (= (:status response) 403))
        (is (not (contains? (:headers response) "Set-Cookie")))
        )
      )

    (testing "invalid password"
      (let [response
            ((bind-app db)
             (->
              (request :post "/api/v1/login")
              (content-type "application/json")
              (body (json/write-str {:username "some_user"
                                     :password "wrong_pass"}))
              ))]
        (is (= (:status response) 403))
        (is (not (contains? (:headers response) "Set-Cookie")))
        )
      )
    )
  
  (testing "not-found route"
    (let [response ((bind-app db) (request :get "/invalid"))]
      (is (= (:status response) 404)))))
