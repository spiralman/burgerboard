(ns burgerboard-web.app
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(def initial-state
  (atom
   {:groups []
    :boards []
    }
   )
  )

(defn group [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/div nil {})
      )
    )
  )

(defn group-nav [data owner]
  (reify
    om/IRender
    (render [this]
      (apply dom/ul #js {:className "groups"}
             (map (fn [group-data] (om/build group group-data)) data)
             )
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
              (om/build group-nav (:groups data))
              (om/build board (:boards data))
              )
      )
    )
  )

(defn main []
  (om/root app {:text "Hello world!"}
           {:target (. js/document (getElementById "burgerboard"))})
  )
