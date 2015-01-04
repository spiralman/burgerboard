(ns burgerboard-web.group-nav
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! <! chan]]
            [burgerboard-web.widgets :as widgets]
            [burgerboard-web.api :as api]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(defn board-item [data owner {:keys [boards_url select-board]}]
  (reify
    om/IInitState
    (init-state [this]
      {:new-value (chan)})
    om/IWillMount
    (will-mount [this]
      (go (let [new-value (<! (om/get-state owner :new-value))
                new-board (<! (api/json-post boards_url
                                             {:name new-value}))]
            (om/transact! data (fn [_] (dissoc new-board :group)))
            ))
      )
    om/IRenderState
    (render-state [this state]
      (dom/li #js {:className "board-item"}
              (if-not (contains? data :id)
                (om/build widgets/save-single-value
                          data
                          {:opts {:className "board-editor"
                                  :k :name
                                  :value-saved (:new-value state)}})
                (dom/a #js {:className "board-link"
                            :href "#"
                            :onClick (fn [_]
                                       (put! select-board @data)
                                       false)}
                       (:name data))
                )
              )
      )
    )
  )

(defn add-board [boards owner]
  (reify
    om/IRender
    (render [this]
      (dom/li #js {:className "board-item"}
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

(defn boards [data owner {:keys [boards_url select-board]}]
  (reify
    om/IRender
    (render [this]
      (apply dom/ul #js {:className "boards"}
             (concat (om/build-all board-item data
                                   {:opts {:boards_url boards_url
                                           :select-board select-board}})
                     [(om/build add-board data)])
             )
      )
    )
  )

(defn group [data owner {:keys [select-board]}]
  (reify
    om/IInitState
    (init-state [this]
      {:new-value (chan)})
    om/IWillMount
    (will-mount [this]
      (go (let [new-value (<! (om/get-state owner :new-value))
                new-group (<! (api/json-post "/api/v1/groups"
                                             {:name new-value}))]
            (om/transact! data (fn [_] new-group))
            ))
      )
    om/IRenderState
    (render-state [this state]
      (dom/li #js {:className "group"}
              (if-not (contains? data :id)
                (om/build widgets/save-single-value
                          data
                          {:opts {:className "group-editor"
                                  :k :name
                                  :value-saved (:new-value state)}})
                (dom/div #js {}
                         (dom/span #js {:className "group-name"}
                                   (:name data))
                         (if-not (contains? data :boards)
                           (om/build widgets/loader
                                     data
                                     {:opts {:load-from :boards_url
                                             :load-into :boards
                                             :load-keys [:boards]}})
                           (om/build boards (:boards data)
                                     {:opts {:boards_url (:boards_url data)
                                             :select-board select-board}})
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

(defn group-nav [data owner {:keys [select-board]}]
  (reify
    om/IRender
    (render [this]
      (apply dom/ul #js {:className "groups"}
             (concat (om/build-all group data
                                   {:opts {:select-board select-board}})
                       [(om/build add-group data)])
             )
      )
    )
  )
