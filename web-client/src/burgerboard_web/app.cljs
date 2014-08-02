(ns burgerboard-web.app
  (:require [om.core :as om :include-macros true]
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

(defn loading []
  (dom/div #js {:className "loading"})
  )

(defn board-nav [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/li #js {:className "board-nav"}
              (:name data)
              )
      )
    )
  )

(defn group [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/li #js {:className "group"}
              (dom/span #js {:className "group-name"}
                        (:name data))
              (apply dom/ul #js {:className "boards"}
                     (om/build-all board-nav (:boards data))
                     )
              )
      )
    )
  )

(defn group-nav [data owner]
  (reify
    om/IRender
    (render [this]
      (apply dom/ul #js {:className "groups"}
             (if (empty? data)
               (list (loading))
               (om/build-all group data)
               )
             )
      )
    )
  )

(defn store [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/li #js {:className "store"}
              (dom/span #js {:className "store-name"}
                        (:name data))
              (dom/span #js {:className "rating-graph"})
              (dom/span #js {:className "rating"}
                        (str (:rating data)))
              )
      )
    )
  )

(defn leaderboard [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/div #js {:className "leaderboard"}
               (dom/div #js {:className "board-title"}
                        (:name data))
               (om/build store
                         (apply max-key :rating (:stores data)))
               (om/build store
                         (apply min-key :rating (:stores data)))
               )
      )
    )
  )

(def descending #(compare %2 %1))

(defn stores [data owner]
  (reify
    om/IRender
    (render [this]
      (apply dom/ul #js {:className "stores"}
             (om/build-all store
                           (sort-by :rating descending data))
             )
      )
    )
  )

(defn board [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/div #js {:className "board"}
               (cond
                (empty? data) nil
                (not (contains? data :stores)) (loading)
                :else (list
                       (om/build leaderboard data)
                       (om/build stores (:stores data)))
                )
               )
      )
    )
  )

(defn app [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/div #js {:className "burgerboard"}
              (om/build group-nav (:groups data))
              (om/build board (:board data))
              )
      )
    )
  )

(defn main []
  (om/root app initial-state
           {:target (. js/document (getElementById "burgerboard"))})
  )

(set! (.-onload js/window) main)
