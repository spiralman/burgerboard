(ns test-burgerboard.test-board
  (:require-macros [cemerick.cljs.test
                    :refer (is deftest with-test testing test-var)]
                   [test-burgerboard.huh]
                   )
  (:require
   [test-burgerboard.huh :refer [rendered tag containing with-class sub-component with-text with-attr after-event rendered-component in setup-state]]
   [burgerboard-web.board :as board]
   [burgerboard-web.widgets :as widgets]
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]
   )
  )

(deftest board-contains-leaderboard-and-store-list
  (is (rendered
       board/board {:id 1
                    :stores [{:id 1} {:id 2}]}
       (tag "div"
            (with-class "board")
            (containing
             (sub-component board/leaderboard {:id 1
                                             :stores [{:id 1} {:id 2}]})
             (sub-component board/stores [{:id 1} {:id 2}])
             )
            )
       )
      )
  )

(deftest board-empty-without-selected-board
  (is (rendered
       board/board {}
       (tag "div"
            (with-class "board")
            )
       )
      )
  )

(deftest board-loading-when-selected-not-populated
  (is (rendered
       board/board {:id 1 :name "Board Name"}
       (tag "div"
            (with-class "board")
            (containing
             (tag "div"
                  (with-class "loading"))
             )
            )
       )
      )
  )

(deftest leaderboard-contains-name-and-top-scores
  (is (rendered
      board/leaderboard {:id 1 :name "Board Name"
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
                 (with-text "Board Name")
                 )
            (sub-component board/store {:id 2
                                      :name "Top Store"
                                      :rating 5})
            (sub-component board/store {:id 3
                                      :name "Bottom Store"
                                      :rating 0.1})
            )
           )
      )
     )
  )

(deftest stores-lists-stores-in-descending-ranking
  (is (rendered
       board/stores [{:id 1
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
             (sub-component board/store
                            {:id 2
                             :name "Top Store"
                             :rating 5})
             (sub-component board/store
                            {:id 1
                             :name "Store 1"
                             :rating 1})
             (sub-component board/store
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
       board/store {:id 1
                    :name "Store"
                    :rating 2}
       (tag "li"
            (with-class "store")
            (containing
             (tag "span"
                  (with-class "store-name")
                  (with-text "Store"))
             (tag "span"
                  ;; Placeholder for progress bar
                  (with-class "rating-graph"))
             (tag "span"
                  (with-class "rating")
                  (with-text "2")
                  )
             )
            )
       )
      )
  )

(deftest store-renders-edit-store-without-id
  (is (rendered
       board/store {:name "Store"}
       (tag "li"
            (with-class "store")
            (containing
             (sub-component board/store-editor {:name "Store"})
             )
            )
       )
      )
  )

(deftest store-editor-renders-editing-controns
  (is (rendered
       board/store-editor {:name "Store"}
       (tag "span"
            (with-class "store-editor")
            (containing
             (sub-component widgets/text-editor
                            {:name "Store"}
                            {:opts {:attr :name
                                    :className "store-name-editor"}})
             (tag "button"
                  (with-class "save-store")
                  (with-attr "type" "button")
                  (with-text "Save"))
             )
            )
       )
      )
  )
