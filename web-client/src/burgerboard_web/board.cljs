(ns burgerboard-web.board
  (:require [burgerboard-web.widgets :as widgets]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(defn store [data owner]
  (reify
    om/IRender
    (render [this]
      (apply dom/li #js {:className "store"}
              (if-not (contains? data :id)
                (list (om/build widgets/save-single-value
                                data
                                {:opts {:className "store-editor"
                                        :k :name}}))
                (list
                 (dom/span #js {:className "store-name"}
                           (:name data))
                 (dom/span #js {:className "rating-graph"})
                 (dom/span #js {:className "rating"}
                           (str (:rating data)))
                 )
                )
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
      (apply dom/div #js {:className "board"}
               (cond
                (empty? data) (list nil)
                (not (contains? data :stores)) (list (widgets/loading))
                :else (list
                       (om/build leaderboard data)
                       (om/build stores (:stores data)))
                )
               )
      )
    )
  )
