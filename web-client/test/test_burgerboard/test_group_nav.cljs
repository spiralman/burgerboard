(ns test-burgerboard.test-group-nav
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [cemerick.cljs.test
                    :refer (is deftest with-test testing test-var done)]
                   [test-burgerboard.huh :refer [with-rendered]]
                   )
  (:require
   [cljs.core.async :refer [<! chan put!]]
   [test-burgerboard.huh :refer [rendered tag containing with-class with-attr
                                 sub-component with-text setup-state in
                                 rendered-component after-event]]
   [test-burgerboard.fake-server :refer [expect-request json-response]]
   [burgerboard-web.group-nav :as group-nav]
   [burgerboard-web.widgets :as widgets]
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]
   )
  )

(deftest group-nav-contains-groups
  (is (rendered
       group-nav/group-nav [{:id 1}
                            {:id 2}]
       {:opts {:select-board "select board channel"}}
       (tag "ul"
            (with-class "groups")
            (containing
             (sub-component group-nav/group {:id 1}
                            {:opts {:select-board "select board channel"}
                             :om.core/index 0})
             (sub-component group-nav/group {:id 2}
                            {:opts {:select-board "select board channel"}
                             :om.core/index 1})
             (sub-component group-nav/add-group [{:id 1} {:id 2}])
             )
            )
       )
      )
  )

(deftest group-nav-contains-just-add-with-empty-group-list
  (is (rendered
       group-nav/group-nav []
       (tag "ul"
            (with-class "groups")
            (containing
             (sub-component group-nav/add-group [])
             )
            )
       )
      )
  )

(deftest add-group-displays-button-for-adding-group
  (is (rendered
       group-nav/add-group []
       (tag "li"
            (with-class "group")
            (containing
             (tag "button"
                  (with-class "add-group")
                  (with-attr "type" "button")
                  (with-text "Add Group")
                  )
             )
            )
       )
      )
  )

(deftest add-group-appends-new-group-to-groups
  (let [state (setup-state [{:id 1 :name "first"}])]
    (after-event
     :click #js {:target #js {}}
     (in (rendered-component
          group-nav/add-group state)
         "button")
     (fn [_]
       (is (= [{:id 1 :name "first"} {:name ""}] @state))
       )
     )
    )
  )

(deftest group-without-boards-contains-boards-loader
  (is (rendered
       group-nav/group {:id 1
                        :name "Some Group"
                        :boards_url "/api/v1/groups/1/boards"}
       (tag "li"
            (with-class "group")
            (containing
             (tag "div"
                  (containing
                   (tag "span"
                        (with-class "group-name")
                        (with-text "Some Group"))
                   (sub-component widgets/loader
                                  {:id 1
                                   :name "Some Group"
                                   :boards_url "/api/v1/groups/1/boards"}
                                  {:opts {:load-from :boards_url
                                          :load-into :boards
                                          :load-keys [:boards]}})
                   )
                  )
             )
            )
       )
      )
  )

(deftest group-contains-boards
  (is (rendered
       group-nav/group {:id 1
                        :name "Some Group"
                        :boards_url "http://boards_url"
                        :boards [{:id 1} {:id 2}]
                        }
       {:opts {:select-board "select board channel"}}
       (tag "li"
            (with-class "group")
            (containing
             (tag "div"
                  (containing
                   (tag "span"
                        (with-class "group-name")
                        (with-text "Some Group"))
                   (sub-component group-nav/boards [{:id 1} {:id 2}]
                                  {:opts {:boards_url "http://boards_url"
                                          :select-board "select board channel"}})
                   )
                  )
             )
            )
       )
      )
  )

(deftest boards-contains-just-add-with-empty-board-list
  (is (rendered
       group-nav/boards []
       (tag "ul"
            (with-class "boards")
            (containing
             (sub-component group-nav/add-board [])
             )
            )
       )
      )
  )

(deftest boards-contains-boards-and-add-board
  (is (rendered
       group-nav/boards
       [{:id 1} {:id 2}]
       {:opts {:boards_url "http://boards_url"
               :select-board "select board channel"}}
       (tag "ul"
            (with-class "boards")
            (containing
             (sub-component group-nav/board-item {:id 1}
                            {:opts {:boards_url "http://boards_url"
                                    :select-board "select board channel"}
                                    :om.core/index 0})
             (sub-component group-nav/board-item {:id 2}
                            {:opts {:boards_url "http://boards_url"
                                    :select-board "select board channel"}
                                    :om.core/index 1})
             (sub-component group-nav/add-board [{:id 1} {:id 2}])
             )
            )
       )
      )
  )

(deftest group-contains-editor-without-id
  (is (rendered
       group-nav/group {:name "Some Group"}
       (with-rendered [group]
         (tag "li"
              (with-class "group")
              (containing
               (sub-component widgets/save-single-value
                              {:name "Some Group"}
                              {:opts {:className "group-editor"
                                      :k :name
                                      :value-saved (om/get-state group
                                                                 :new-value)}})
               )
              )
         )
       )
      )
  )

(deftest ^:async group-posts-new-group-when-user-saves
  (let [state (setup-state {:name ""})
        group (rendered-component
               group-nav/group state)
        new-value (om/get-state group :new-value)
        responded (expect-request
                   -test-ctx
                   {:method "POST"
                    :url "/api/v1/groups"
                    :json-data {:name "New Group"}}
                   (json-response
                    201
                    {:id 1
                     :name "New Group"
                     :boards_url "http://localhost/api/v1/groups/1"
                     :members_url "http://localhost/api/v1/groups/1/members"})
                   )]
    (put! new-value "New Group")
    (go
     (<! responded)
     (is (= {:id 1
             :name "New Group"
             :boards_url "http://localhost/api/v1/groups/1"
             :members_url "http://localhost/api/v1/groups/1/members"}
            @state))
     (done)
     )
    )
  )

(deftest board-item-links-to-board
  (is (rendered
       group-nav/board-item {:id 1 :name "Board Name"}
       (tag "li"
            (with-class "board-item")
            (containing
             (tag "a"
                  (with-class "board-link")
                  (with-attr "href" "#")
                  (with-text "Board Name")
                  ))
            )
       )
      )
  )

(deftest ^:async clicking-board-item-selects-board
  (let [state (setup-state {:id 1 :name "first"})
        select-board (chan)]
    (after-event
     :click #js {:target #js {}}
     (in (rendered-component
          group-nav/board-item state
          {:opts {:select-board select-board}})
         "a.board-link")
     (fn [_]
       (go (let [selected (<! select-board)]
             (is (= {:id 1 :name "first"} selected))
             (done)
             ))
       )
     )
    )
  )

(deftest board-item-shows-board-editor-without-id
  (is (rendered
       group-nav/board-item {:name "Board Name"}
       (with-rendered [board-item]
         (tag "li"
              (containing
               (sub-component widgets/save-single-value
                              {:name "Board Name"}
                              {:opts {:className "board-editor"
                                      :k :name
                                      :value-saved (om/get-state board-item
                                                                 :new-value)}})
               )
              )
         )
       )
      )
  )

(deftest ^:async board-item-posts-new-group-when-user-saves
  (let [state (setup-state {:name ""})
        board-item (rendered-component
                    group-nav/board-item state
                    {:opts {:boards_url "/api/v1/groups/1/boards"}})
        new-value (om/get-state board-item :new-value)
        responded (expect-request
                   -test-ctx
                   {:method "POST"
                    :url "/api/v1/groups/1/boards"
                    :json-data {:name "New Board"}}
                   (json-response
                    201
                    {:id 1
                     :name "New Board"
                     :url "http://localhost/api/v1/groups/1/boards/1"
                     :stores_url "http://localhost/api/v1/groups/1/boards/1/stores"
                     :group {:id 1 :name "Group"}})
                   )]
    (put! new-value "New Board")
    (go
     (<! responded)
     (is (= {:id 1
             :name "New Board"
             :url "http://localhost/api/v1/groups/1/boards/1"
             :stores_url "http://localhost/api/v1/groups/1/boards/1/stores"}
            @state))
     (done)
     )
    )
  )

(deftest add-board-displays-button-for-adding-board
  (is (rendered
       group-nav/add-board []
       (tag "li"
            (with-class "board")
            (containing
             (tag "button"
                  (with-class "add-board")
                  (with-attr "type" "button")
                  (with-text "Add Board")
                  )
             )
            )
       )
      )
  )

(deftest add-board-appends-new-board-to-boards
  (let [state (setup-state [{:id 1 :name "first"}])]
    (after-event
     :click #js {:target #js {}}
     (in (rendered-component
          group-nav/add-board state)
         "button")
     (fn [_]
       (is (= [{:id 1 :name "first"} {:name ""}] @state))
       )
     )
    )
  )
