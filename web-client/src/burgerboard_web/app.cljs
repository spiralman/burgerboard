(ns burgerboard-web.app
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(defn group-nav [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/div nil {})
      )
    )
  )

(defn board [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/div nil {})
      )
    )
  )

(defn app [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/div #js {:className "burgerboard"}
              (om/build group-nav {})
              (om/build board {})
              )
      )
    )
  )

(defn main []
  (om/root app {:text "Hello world!"}
           {:target (. js/document (getElementById "burgerboard"))})
  )
