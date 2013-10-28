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

(deftest tallying
  (testing "Tallies store ratings"
    (is (= {:id 1 :name "board"
            :stores [{:name "Store1"
                      :id 1
                      :rating 1.5
                      :ratings [{:user_email "owner@example.com"
                                 :rating 1}
                                {:user_email "some_user@example.com"
                                 :rating 2}]}
                     {:name "Store2"
                      :id 2
                      :rating 2.0
                      :ratings [{:user_email "owner@example.com"
                                 :rating 2}
                                {:user_email "some_user@example.com"
                                 :rating nil}]
                      }]}
           (tally-board
            [{:name "Store1"
              :id 1
              :ratings [{:user_email "owner@example.com"
                         :rating 1}
                        {:user_email "some_user@example.com"
                         :rating 2}]}
             {:name "Store2"
              :id 2
              :ratings [{:user_email "owner@example.com"
                         :rating 2}
                        {:user_email "some_user@example.com"
                         :rating nil}]
              }]
            {:id 1 :name "board"})))
    )
  
  (testing "Tallies single store"
    (is (= {:name "Store1"
            :id 1
            :rating 1.5
            :ratings [{:user_email "owner@example.com"
                       :rating 1}
                      {:user_email "some_user@example.com"
                       :rating 2}]}
           (tally-store
            {:name "Store1"
              :id 1
              :ratings [{:user_email "owner@example.com"
                         :rating 1}
                        {:user_email "some_user@example.com"
                         :rating 2}]}
            )))
    )

  (testing "Tally handles all nil ratings"
    (is (= {:name "Store1"
            :id 1
            :rating nil
            :ratings [{:user_email "owner@example.com"
                       :rating nil}
                      {:user_email "some_user@example.com"
                       :rating nil}]}
           (tally-store
            {:name "Store1"
              :id 1
              :ratings [{:user_email "owner@example.com"
                         :rating nil}
                        {:user_email "some_user@example.com"
                         :rating nil}]}
            )))
    )
  )
         