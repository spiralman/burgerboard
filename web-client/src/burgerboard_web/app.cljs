(ns burgerboard-web.app
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(def initial-state
  (atom
   {:groups []
    :board nil
    }
   )
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
                     (map (fn [board-data] (om/build board-nav board-data))
                          (:boards data))
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
             (map (fn [group-data] (om/build group group-data)) data)
             )
      )
    )
  )

(defn leaderboard [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/div nil {})
      )
    )
  )

(defn stores [data owner]
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
      (dom/div #js {:className "board"}
               (om/build leaderboard data)
               (om/build stores (:stores data))
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
  (om/root app {:text "Hello world!"}
           {:target (. js/document (getElementById "burgerboard"))})
  )
