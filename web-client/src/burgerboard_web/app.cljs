(ns burgerboard-web.app
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(defn sub-component [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/div nil {:text "sub-component"})
      )
    )
  )

(defn app [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/h1 #js {:className "app"} (:text data)
              (om/build sub-component {})
              (dom/span nil (:text "test"))
              (dom/span nil (:text "input"))
              )
      )
    )
  )

(defn main []
  (om/root app {:text "Hello world!"}
           {:target (. js/document (getElementById "burgerboard"))})
  )
