(ns burgerboard.test.users-test
  (:use clojure.test
        burgerboard.users)
  )

(deftest test-users
  (testing "User creation and password verification"
    (let [user (create-user "user@example.com" "password" "Some Name")]
      (is (= "user@example.com" (:email user)))
      (is (= "Some Name" (:name user)))
      (is (not (= "password" (:password user))))

      (is (login-valid user "user@example.com" "password"))
      (is (not (login-valid user "user@example.com" "other password")))
      (is (not (login-valid user "other-user@example.com" "password")))
      (is (not (login-valid nil "other-user@example.com" "password")))
      )
    (is (= nil (create-user "bad email" "password" "Name")))
    )
  )

(deftest test-create-group
  (let [group (create-group "New Group" {:email "owner@example.com"
                                         :name "Owner"})]
    (is (= "New Group" (:name group)))
    (is (= "owner@example.com" (:owner group)))
    )
  )
