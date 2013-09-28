(ns burgerboard.test.users-test
  (:use clojure.test
        burgerboard.users)
  )

(deftest test-users
  (testing "User creation and password verification"
    (let [user (create-user "username" "password")]
      (is (not (= "password" (:password user))))
      (is (login-valid user "username" "password"))
      (is (not (login-valid user "username" "other password")))
      (is (not (login-valid user "other-user" "password")))
      (is (not (login-valid nil "other-user" "password")))
      )
    )
  )