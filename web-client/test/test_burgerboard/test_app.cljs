(ns test-burgerboard.test-app
  (:require-macros [cemerick.cljs.test
                    :refer (is deftest with-test testing test-var)]
                   [test-burgerboard.huh]
                   )
  (:require
   [test-burgerboard.huh :refer [rendered, tag, containing, with-class, sub-component, text]]
   [burgerboard-web.app :as app]
   )
  )

(deftest app-contains-sub-components
  (is (rendered
       app/app {:groups [{:id 1}]
                :boards [{:id 1}]}
       (tag "div"
            (with-class "burgerboard")
            (containing
             (sub-component app/group-nav [{:id 1}])
             (sub-component app/board [{:id 1}])
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
       (tag "span"
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
