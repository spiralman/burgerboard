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
               (list (widgets/loading))
               (om/build-all group data)
               )
             )
      )
    )
  )
