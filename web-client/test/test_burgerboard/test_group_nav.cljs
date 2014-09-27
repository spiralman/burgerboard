(ns test-burgerboard.test-group-nav
  (:require-macros [cemerick.cljs.test
                    :refer (is deftest with-test testing test-var)]
                   [test-burgerboard.huh]
                   )
  (:require
   [test-burgerboard.huh :refer [rendered tag containing with-class with-attr
                                 sub-component text nothing]]
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

(deftest group-contains-board-item
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
                   (sub-component group-nav/board-item {:id 1})
                   (sub-component group-nav/board-item {:id 2})
                   )
                  )
             )
            )
       )
      )
  )

(deftest group-contains-editor-without-id
  (is (rendered
       group-nav/group {:name "Some Group"}
       (tag "li"
            (with-class "group")
            (containing
             (sub-component group-nav/group-editor {:name "Some Group"})
             )
            )
       )
      )
  )

(deftest group-editor-contains-name-editor-and-save
  (is (rendered
       group-nav/group-editor {:name "Some Group"}
       (tag "span"
            (with-class "group-editor")
            (containing
             (tag "input"
                  (with-class "group-name-editor")
                  (with-attr "type" "text")
                  (containing nothing)
                  )
             (tag "button"
                  (with-class "save-group")
                  (with-attr "type" "button")
                  (containing
                   (text "Save")
                   )
                  )
             )
            )
       )
      )
  )

(deftest board-item-links-to-board
  (is (rendered
       group-nav/board-item {:id 1 :name "Board Name"}
       (tag "li"
            (with-class "board-item")
            (containing
             (text "Board Name")
             )
            )
       )
      )
  )

(deftest board-item-shows-board-editor-without-id
  (is (rendered
       group-nav/board-item {:name "Board Name"}
       (tag "li"
            (containing
             (sub-component group-nav/board-editor {:name "Board Name"})
             )
            )
       )
      )
  )

(deftest board-editor-shows-board-editing-controls
  (is (rendered
       group-nav/board-editor {:name "Board Name"}
       (tag "div"
            (with-class "board-editor")
            (containing
             (tag "input"
                  (with-class "board-name-editor")
                  (with-attr "type" "text")
                  (containing nothing)
                  )
             (tag "button"
                  (with-class "save-board")
                  (with-attr "type" "button")
                  (containing
                   (text "Save")
                   )
                  )
             )
            )
       )
      )
  )
