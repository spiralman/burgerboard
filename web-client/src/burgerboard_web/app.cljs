(ns burgerboard-web.app
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(defn app [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/h1 nil (:text data)
              (dom/span (:text "test"))
              (dom/span (:text "input"))
              )
      )
    )
  )

(defn main []
  (om/root app {:text "Hello world!"}
           {:target (. js/document (getElementById "burgerboard"))})
  )
