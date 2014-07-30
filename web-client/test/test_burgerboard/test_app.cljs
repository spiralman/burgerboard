(ns test-burgerboard.test-app
  (:require-macros [cemerick.cljs.test
                    :refer (is deftest with-test testing test-var)]
                   [test-burgerboard.huh]
                   )
  (:require
   [test-burgerboard.huh :refer [rendered, tag, containing, with-class, sub-component, text]]
   [burgerboard-web.app :as app]
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]
   )
  )

(deftest app-contains-sub-components
  (is (rendered
       app/app {:groups [{:id 1}]
                :board {:id 1}}
       (tag "div"
            (with-class "burgerboard")
            (containing
             (sub-component app/group-nav [{:id 1}])
             (sub-component app/board {:id 1})
             )
            )
       )
      )
  )

(deftest group-nav-contains-groups
  (is (rendered
       app/group-nav [{:id 1}
                      {:id 2}]
       (tag "ul"
            (with-class "groups")
            (containing
             (sub-component app/group {:id 1})
             (sub-component app/group {:id 2})
             )
            )
       )
      )
  )

(deftest group-contains-board-nav
  (is (rendered
       app/group {:id 1
                  :name "Some Group"
                  :boards [{:id 1} {:id 2}]
                  }
       (tag "li"
            (with-class "group")
            (containing
             (tag "span"
                  (with-class "group-name")
                  (containing
                   (text "Some Group")
                   )
                  )
             (tag "ul"
                  (with-class "boards")
                  (containing
                   (sub-component app/board-nav {:id 1})
                   (sub-component app/board-nav {:id 2})
                   )
                  )
             )
            )
       )
      )
  )

(deftest board-nav-links-to-board
  (is (rendered
       app/board-nav {:id 1 :name "Board Name"}
       (tag "li"
            (with-class "board-nav")
            (containing
             (text "Board Name")
             )
            )
       )
      )
  )

(deftest board-contains-leaderboard-and-store-list
  (is (rendered
       app/board {:id 1
                  :stores [{:id 1} {:id 2}]}
       (tag "div"
            (with-class "board")
            (containing
             (sub-component app/leaderboard {:id 1
                                             :stores [{:id 1} {:id 2}]})
             (sub-component app/stores [{:id 1} {:id 2}])
             )
            )
       )
      )
  )

(deftest leaderboard-contains-name-and-top-scores
  (is (rendered
      app/leaderboard {:id 1 :name "Board Name"
                       :stores
                       [{:id 1
                         :name "Store 1"
                         :rating 1}
                        {:id 2
                         :name "Top Store"
                         :rating 5}
                        {:id 3
                         :name "Bottom Store"
                         :rating 0.1}]}
      (tag "div"
           (with-class "leaderboard")
           (containing
            (tag "div"
                 (with-class "board-title")
                 (containing (text "Board Name"))
                 )
            (sub-component app/store {:id 2
                                      :name "Top Store"
                                      :rating 5})
            (sub-component app/store {:id 3
                                      :name "Bottom Store"
                                      :rating 0.1})
            )
           )
      )
     )
  )

(deftest stores-lists-stores-in-descending-ranking
  (is (rendered
       app/stores [{:id 1
                    :name "Store 1"
                    :rating 1}
                   {:id 2
                    :name "Top Store"
                    :rating 5}
                   {:id 3
                    :name "Bottom Store"
                    :rating 0.1}]

       (tag "ul"
            (with-class "stores")
            (containing
             (sub-component app/store
                            {:id 2
                             :name "Top Store"
                             :rating 5})
             (sub-component app/store
                            {:id 1
                             :name "Store 1"
                             :rating 1})
             (sub-component app/store
                            {:id 3
                             :name "Bottom Store"
                             :rating 0.1})
             )
            )
       )
      )
  )

(deftest store-renders-name-and-score
  (is (rendered
       app/store {:id 1
                  :name "Store"
                  :rating 2}
       (tag "li"
            (with-class "store")
            (containing
             (tag "span"
                  (with-class "store-name")
                  (containing (text "Store")))
             (tag "span"
                  ;; Placeholder for progress bar
                  (with-class "rating-graph"))
             (tag "span"
                  (with-class "rating")
                  (containing (text "2"))
                  )
             )
            )
       )
      )
  )
