(ns burgerboard-web.group-nav
  (:require [burgerboard-web.widgets :as widgets]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(defn board-item [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/li #js {:className "board-item"}
              (if-not (contains? data :id)
                (om/build widgets/save-single-value
                          data
                          {:opts {:className "board-editor"
                                  :k :name}})
                (:name data)
                )
              )
      )
    )
  )

(defn add-board [boards owner]
  (reify
    om/IRender
    (render [this]
      (dom/li #js {:className "board"}
              (dom/button #js {:className "add-board"
                               :type "button"
                               :onClick (fn [_] (om/transact!
                                                 boards
                                                 (fn [_] (conj @boards
                                                               {:name ""}))))}
                          "Add Board")
              )
      )
    )
  )

(defn group [data owner]
  (reify
    om/IRender
    (render [this]
      (apply dom/li #js {:className "group"}
              (if-not (contains? data :id)
                (list (om/build widgets/save-single-value
                                data
                                {:opts {:className "group-editor"
                                        :k :name}}))
                (list
                 (dom/span #js {:className "group-name"}
                           (:name data))
                 (apply dom/ul #js {:className "boards"}
                        (concat (om/build-all board-item (:boards data))
                                [(om/build add-board (:boards data))])
                        )
                 )
                )
              )
      )
    )
  )

(defn add-group [groups owner]
  (reify
    om/IRender
    (render [this]
      (dom/li #js {:className "group"}
              (dom/button #js {:className "add-group"
                               :type "button"
                               :onClick (fn [_] (om/transact!
                                                 groups
                                                 (fn [_] (conj @groups
                                                               {:name ""}))))}
                          "Add Group")
              )
      )
    )
  )

(defn group-nav [data owner]
  (reify
    om/IRender
    (render [this]
      (apply dom/ul #js {:className "groups"}
             (if (nil? data)
               (list (widgets/loading))
               (concat (om/build-all group data)
                       [(om/build add-group data)])
               )
             )
      )
    )
  )
