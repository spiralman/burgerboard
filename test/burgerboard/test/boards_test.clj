(ns burgerboard.test.boards-test
  (:use clojure.test
        burgerboard.boards)
  )

(deftest boards
  (testing "Creating a board"
    (is (= {:name "board"
            :group {:id 1 :name "group"}}
           (create-board "board" {:id 1 :name "group"})))
    )
  )