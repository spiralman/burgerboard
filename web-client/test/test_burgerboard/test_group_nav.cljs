(ns test-burgerboard.test-group-nav
  (:require-macros [cemerick.cljs.test
                    :refer (is deftest with-test testing test-var)]
                   [test-burgerboard.huh]
                   )
  (:require
   [test-burgerboard.huh :refer [rendered tag containing with-class sub-component text nothing]]
   [burgerboard-web.group-nav :as group-nav]
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]
   )
  )

(deftest group-nav-contains-groups
  (is (rendered
       group-nav/group-nav [{:id 1}
                      {:id 2}]
       (tag "ul"
            (with-class "groups")
            (containing
             (sub-component group-nav/group {:id 1})
             (sub-component group-nav/group {:id 2})
             )
            )
       )
      )
  )

(deftest group-nav-contains-loading-indicator-without-groups
  (is (rendered
       group-nav/group-nav []
       (tag "ul"
            (with-class "groups")
            (containing
             (tag "div"
                  (with-class "loading"))
             )
            )
       )
      )
  )

(deftest group-contains-board-nav
  (is (rendered
       group-nav/group {:id 1
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
                   (sub-component group-nav/board-nav {:id 1})
                   (sub-component group-nav/board-nav {:id 2})
                   )
                  )
             )
            )
       )
      )
  )

(deftest board-nav-links-to-board
  (is (rendered
       group-nav/board-nav {:id 1 :name "Board Name"}
       (tag "li"
            (with-class "board-nav")
            (containing
             (text "Board Name")
             )
            )
       )
      )
  )
