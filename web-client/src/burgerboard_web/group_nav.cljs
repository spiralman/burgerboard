(ns burgerboard-web.group-nav
  (:require [burgerboard-web.widgets :as widgets]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

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

(defn group-editor [group owner]
  (reify
    om/IRender
    (render [this]
      (dom/span #js {:className "group-editor"}
                (dom/input #js {:className "group-name-editor"
                                :type "text"})
                (dom/button #js {:className "save-group"
                                 :type "button"}
                            "Save")
                )
      )
    )
  )

(defn group [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/li #js {:className "group"}
              (if-not (contains? data :id)
                (om/build group-editor data)
                (list
                 (dom/span #js {:className "group-name"}
                           (:name data))
                 (apply dom/ul #js {:className "boards"}
                        (om/build-all board-nav (:boards data))
                        )
                 )
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
               (list (widgets/loading))
               (om/build-all group data)
               )
             )
      )
    )
  )
