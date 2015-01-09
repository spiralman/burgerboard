(ns test-burgerboard.test-board
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [cemerick.cljs.test
                    :refer (is deftest with-test testing test-var done)]
                   [test-burgerboard.huh :refer [with-rendered]]
                   )
  (:require
   [cljs.core.async :refer [<! put! chan]]
   [test-burgerboard.huh :refer [rendered tag containing with-class sub-component with-text with-attr with-prop has-attr after-event rendered-component in setup-state]]
   [test-burgerboard.fake-server :refer [expect-request json-response]]
   [burgerboard-web.board :as board]
   [burgerboard-web.widgets :as widgets]
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]
   )
  )

(deftest board-contains-leaderboard-and-store-list
  (is (rendered
       board/board {:id 1
                    :name "Board Name"
                    :stores_url "http://stores"
                    :stores [{:id 1} {:id 2}]}
       {:opts {:user-email "user@email.com"}}
       (tag "div"
            (with-class "board")
            (containing
             (tag "h1"
                  (with-class "board-title")
                  (with-text "Board Name"))
             (sub-component board/leaderboard {:id 1
                                               :name "Board Name"
                                               :stores_url "http://stores"
                                               :stores [{:id 1} {:id 2}]}
                            {:opts {:user-email "user@email.com"}})
             (sub-component board/stores [{:id 1} {:id 2}]
                            {:opts {:stores-url "http://stores"
                                    :user-email "user@email.com"}})
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
       board/board {:id 1 :name "Board Name" :stores_url "http://stores"}
       (tag "div"
            (with-class "board")
            (containing
             (tag "h1"
                  (with-class "board-title")
                  (with-text "Board Name"))
             (sub-component widgets/loader
                            {:id 1 :name "Board Name"
                             :stores_url "http://stores"}
                            {:opts {:load-from :url
                                    :load-into :stores
                                    :load-keys [:stores]}}))
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
      {:opts {:user-email "user@email.com"}}
      (tag "ol"
           (with-class "leaderboard")
           (containing
            (sub-component board/store {:id 2
                                        :name "Top Store"
                                        :rating 5}
                           {:opts {:user-email "user@email.com"}})
            (sub-component board/store {:id 3
                                        :name "Bottom Store"
                                        :rating 0.1}
                           {:opts {:user-email "user@email.com"}})
            )
           )
      )
     )
  )

(deftest leaderboard-skips-unsaved-stores
  (is (rendered
       board/leaderboard {:id 1 :name "Board Name"
                          :stores
                          [{:id 1
                            :name "Store 1"
                            :rating 1}
                           {:name "Top Store"
                            :rating 5}
                           {:id 3
                            :name "Bottom Store"
                            :rating 0.1}]}
       {:opts {:user-email "user@email.com"}}
      (tag "ol"
           (with-class "leaderboard")
           (containing
            (sub-component board/store {:id 1
                                        :name "Store 1"
                                        :rating 1}
                           {:opts {:user-email "user@email.com"}})
            (sub-component board/store {:id 3
                                        :name "Bottom Store"
                                        :rating 0.1}
                           {:opts {:user-email "user@email.com"}})
            )
           )
      )
     )
  )

(deftest leaderboard-shows-placeholder-with-less-than-2-saved-and-ranked-stores
  (is (rendered
      board/leaderboard {:id 1 :name "Board Name"
                       :stores
                       [{:id 1
                         :name "Store 1"
                         :rating 1}
                        {:id 2
                         :name "Store 2"
                         :rating nil}
                        {:name ""}]}
      (tag "ol"
           (with-class "leaderboard")
           (containing
            (tag "div"
                 (with-class "store-teaser")
                 (with-text "Add some more places!")
                 )
            ))
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
       {:opts {:stores-url "http://stores"
               :user-email "user@email.com"}}
       (tag "ol"
            (with-class "stores")
            (containing
             (sub-component board/store
                            {:id 2
                             :name "Top Store"
                             :rating 5}
                            {:opts {:stores-url "http://stores"
                                    :user-email "user@email.com"}
                             :om.core/index 0})
             (sub-component board/store
                            {:id 1
                             :name "Store 1"
                             :rating 1}
                            {:opts {:stores-url "http://stores"
                                    :user-email "user@email.com"}
                             :om.core/index 1})
             (sub-component board/store
                            {:id 3
                             :name "Bottom Store"
                             :rating 0.1}
                            {:opts {:stores-url "http://stores"
                                    :user-email "user@email.com"}
                             :om.core/index 2})
             (sub-component board/add-store
                            [{:id 1
                              :name "Store 1"
                              :rating 1}
                             {:id 2
                              :name "Top Store"
                              :rating 5}
                             {:id 3
                              :name "Bottom Store"
                              :rating 0.1}])
             )
            )
       )
      )
  )

(deftest stores-contains-just-add-with-empty-list
  (is (rendered
       board/stores []
       (tag "ol"
            (with-class "stores")
            (containing
             (sub-component board/add-store [])
             )
            )
       )
      )
  )

(deftest add-store-displays-button-for-adding-store
  (is (rendered
       board/add-store []
       (tag "li"
            (with-class "store")
            (containing
             (tag "button"
                  (with-class "add-store")
                  (with-attr "type" "button")
                  (with-text "Add Store")
                  )
             )
            )
       )
      )
  )

(deftest add-store-appends-new-store-to-stores
  (let [state (setup-state [{:id 1 :name "first"}])]
    (after-event
     :click #js {:target #js {}}
     (in (rendered-component
          board/add-store state)
         "button")
     (fn [_]
       (is (= [{:id 1 :name "first"} {:name ""}] @state))
       )
     )
    )
  )

(deftest store-renders-name-and-score
  (is (rendered
       board/store {:id 1
                    :name "Store"
                    :rating 2
                    :ratings
                    [{:user_email "user@email.com"
                      :rating nil}]}
       {:opts {:user-email "user@email.com"}}
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
                  (with-text "2"))
             (tag "select"
                  (with-class "rating-option")
                  (with-prop "value" "?")
                  (containing
                   (tag "option"
                        (with-attr "value" "?")
                        (with-text "?"))
                   (tag "option"
                        (with-attr "value" "1")
                        (with-text "1"))
                   (tag "option"
                        (with-attr "value" "2")
                        (with-text "2"))
                   (tag "option"
                        (with-attr "value" "3")
                        (with-text "3"))
                   (tag "option"
                        (with-attr "value" "4")
                        (with-text "4"))
                   (tag "option"
                        (with-attr "value" "5")
                        (with-text "5"))
                   ))
             )
            )
       )
      ))

(deftest store-selects-value-when-set-by-user
  (is (rendered
       board/store {:id 1
                    :name "Store"
                    :rating 2
                    :ratings
                    [{:user_email "user@email.com"
                      :rating 3}]}
       {:opts {:user-email "user@email.com"}}
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
                  (with-text "2"))
             (tag "select"
                  (with-class "rating-option")
                  (with-prop "value" 3)
                  (containing
                   (tag "option"
                        (with-attr "value" "?")
                        (with-text "?"))
                   (tag "option"
                        (with-attr "value" "1")
                        (with-text "1"))
                   (tag "option"
                        (with-attr "value" "2")
                        (with-text "2"))
                   (tag "option"
                        (with-attr "value" "3")
                        (with-text "3"))
                   (tag "option"
                        (with-attr "value" "4")
                        (with-text "4"))
                   (tag "option"
                        (with-attr "value" "5")
                        (with-text "5"))
                   ))
             )
            )
       )
      ))

(deftest ^:async store-posts-new-rating-on-change
  (let [state (setup-state
               {:id 1
                :name "Store"
                :rating_url "http://rating"
                :rating 2
                :ratings
                [{:user_email "user@email.com"
                  :rating nil}]})
        store (rendered-component board/store state
                                  {:opts {:user-email "user@email.com"}})
        responded (expect-request
                   -test-ctx
                   {:method "PUT"
                    :url "http://rating"
                    :json-data {:rating 3}}
                   (json-response
                    200
                    {:id 1
                     :name "Store"
                     :rating_url "http://rating"
                     :rating 2.5
                     :ratings
                     [{:user_email "user@email.com"
                       :rating 3}]}))]
    (after-event
     :change #js {:target #js {:value "3"}}
     (in store
         "select")
     (fn [_]
       (go
        (<! responded)
        (is (= {:id 1
                :name "Store"
                :rating_url "http://rating"
                :rating 2.5
                :ratings
                [{:user_email "user@email.com"
                  :rating 3}]}
               @state))
        (done)
        )))
    ))

(deftest ^:async store-posts-null-rating-on-change-to-?
  (let [state (setup-state
               {:id 1
                :name "Store"
                :rating_url "http://rating"
                :rating 2
                :ratings
                [{:user_email "user@email.com"
                  :rating nil}]})
        store (rendered-component board/store state
                                  {:opts {:user-email "user@email.com"}})
        responded (expect-request
                   -test-ctx
                   {:method "PUT"
                    :url "http://rating"
                    :json-data {:rating nil}}
                   (json-response
                    200
                    {:id 1
                     :name "Store"
                     :rating_url "http://rating"
                     :rating 2
                     :ratings
                     [{:user_email "user@email.com"
                       :rating nil}]}))]
    (after-event
     :change #js {:target #js {:value "?"}}
     (in store
         "select")
     (fn [_]
       (go
        (<! responded)
        (is (= {:id 1
                :name "Store"
                :rating_url "http://rating"
                :rating 2
                :ratings
                [{:user_email "user@email.com"
                  :rating nil}]}
               @state))
        (done)
        )))
    ))

(deftest store-renders-edit-store-without-id
  (is (rendered
       board/store {:name "Store"}
       {:opts {:stores-url "http://stores"}}
       (tag "li"
            (with-class "store")
            (containing
             (sub-component board/store-editor
                            {:name "Store"}
                            {:opts {:stores-url "http://stores"}})
             )
            )
       ))
  )

(deftest store-editor-renders-single-value-editor
  (is (rendered
       board/store-editor {:name "Store"}
       {:opts {:stores-url "http://stores"}}
       (with-rendered [store]
         (tag "div"
              (containing
               (sub-component widgets/save-single-value
                              {:name "Store"}
                              {:opts {:className "store-editor"
                                      :k :name
                                      :value-saved (om/get-state store
                                                                 :new-value)}})
               ))
         )
       )
      )
  )

(deftest ^:async store-posts-new-store-when-user-saves
  (let [state (setup-state {:name ""})
        store (rendered-component
               board/store-editor state
               {:opts {:stores-url "/api/v1/groups/1/boards/1/stores"}})
        new-value (om/get-state store :new-value)
        responded (expect-request
                   -test-ctx
                   {:method "POST"
                    :url "/api/v1/groups/1/boards/1/stores"
                    :json-data {:name "New Store"}}
                   (json-response
                    201
                    {:id 1
                     :name "New Store"
                     :rating_url "/api/v1/groups/1/boards/1/stores/1/rating"
                     :board {:id 1 :name "Board"}})
                   )]
    (put! new-value "New Store")
    (go
     (<! responded)
     (is (= {:id 1
             :name "New Store"
             :rating_url "/api/v1/groups/1/boards/1/stores/1/rating"}
            @state))
     (done)
     )
    )
  )
