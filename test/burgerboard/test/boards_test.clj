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

(deftest stores
  (testing "Creating a store"
    (is (= {:name "store"
            :board {:id 1 :group_id 1}}
           (create-store "store" {:id 1 :group_id 1})))
    )
  )