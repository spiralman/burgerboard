(ns burgerboard-web.app
  (:require [burgerboard-web.widgets :as widgets]
            [burgerboard-web.group-nav :as group-nav]
            [burgerboard-web.board :as board]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(def initial-state
  (atom
   {:groups [{:id 1
              :name "Group"
              :boards_url "http://localhost/api/v1/groups/1/boards"
              :members_url "http://localhost/api/v1/groups/1/members"
              :boards [{:id 1
                        :name "Some Board"
                        :url "http://localhost/api/v1/groups/1/boards/1"
                        :stores_url
                        "http://localhost/api/v1/groups/1/boards/1/stores"
                        :group {:id 1 :name "Group"}
                        }
                       {:id 3
                        :name "New Board"
                        :url "http://localhost/api/v1/groups/1/boards/3"
                        :stores_url
                        "http://localhost/api/v1/groups/1/boards/3/stores"
                        :group {:id 1 :name "Group"}}]}
             {:id 2
              :name "Group2"
              :boards_url "http://localhost/api/v1/groups/2/boards"
              :members_url "http://localhost/api/v1/groups/2/members"}]
    :board {:id 1 :name "Some Board"
            :group {:name "Group" :id 1}
            :url "http://localhost/api/v1/groups/1/boards/1"
            :stores_url "http://localhost/api/v1/groups/1/boards/1/stores"
            :stores [{:name "Store 1"
                      :id 1
                      :rating_url
                      "http://localhost/api/v1/groups/1/boards/1/stores/1/rating"
                      :rating 1.5
                      :ratings [{:user_email "owner@example.com"
                                 :rating 1}
                                {:user_email "some_user@example.com"
                                 :rating 2}]}
                     {:name "Store 2"
                      :id 2
                      :rating_url
                      "http://localhost/api/v1/groups/1/boards/1/stores/2/rating"
                      :rating 2.0
                      :ratings [{:user_email "owner@example.com"
                                 :rating 2}
                                {:user_email "some_user@example.com"
                                 :rating nil}]
                      }]}
    }
   )
  )

(defn app [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/div #js {:className "burgerboard"}
              (om/build group-nav/group-nav (:groups data))
              (om/build board/board (:board data))
              )
      )
    )
  )

(defn main []
  (om/root app initial-state
           {:target (. js/document (getElementById "burgerboard"))})
  )

(set! (.-onload js/window) main)
