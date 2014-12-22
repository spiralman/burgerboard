(ns burgerboard-web.board
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [<! put! chan]]
            [burgerboard-web.widgets :as widgets]
            [burgerboard-web.api :as api]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(defn store [data owner {:keys [stores-url]}]
  (reify
    om/IInitState
    (init-state [this]
      {:new-value (chan)})
    om/IWillMount
    (will-mount [this]
      (go (let [new-value (<! (om/get-state owner :new-value))
                new-store (<! (api/json-post stores-url {:name new-value}))]
            (om/transact! data (fn [_] (dissoc new-store :board)))
            )))
    om/IRenderState
    (render-state [this state]
      (apply dom/li #js {:className "store"}
              (if-not (contains? data :id)
                (list (om/build widgets/save-single-value
                                data
                                {:opts {:className "store-editor"
                                        :k :name
                                        :value-saved (:new-value state)}}))
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
      (apply dom/div #js {:className "leaderboard"}
             (if (< (count (filter #(contains? % :id) (:stores data))) 2)
               (list (dom/div #js {:className "store-teaser"}
                              "Add some more places!"))
               (list
                (om/build store
                          (apply max-key :rating (:stores data)))
                (om/build store
                          (apply min-key :rating (:stores data)))
                )))
      )
    ))

(defn add-store [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/li #js {:className "store"}
              (dom/button #js {:className "add-store"
                               :type "button"
                               :onClick (fn [_] (om/transact!
                                                 data
                                                 (fn [_] (conj @data
                                                               {:name ""}))))}
                          "Add Store"))
      )
    ))

(def descending #(compare %2 %1))

(defn stores [data owner {:keys [stores-url]}]
  (reify
    om/IRender
    (render [this]
      (apply dom/ul #js {:className "stores"}
             (concat
              (om/build-all store
                            (sort-by :rating descending data)
                            {:opts {:stores-url stores-url}})
              (list (om/build add-store data))
             ))
      )
    )
  )

(defn board [data owner]
  (reify
    om/IRender
    (render [this]
      (apply dom/div #js {:className "board"}
             (if-not (empty? data)
               (concat
                (list (dom/h1 #js {:className "board-title"} (:name data)))
                (if-not (contains? data :stores)
                  (list (om/build widgets/loader data
                                  ;; Goofy that we can't do a GET to
                                  ;; stores_url. I wonder who's idea
                                  ;; that was...
                                  {:opts {:load-from :url
                                          :load-into :stores
                                          :load-keys [:stores]}}))
                  (list
                   (om/build leaderboard data)
                   (om/build stores (:stores data)
                             {:opts {:stores-url (:stores_url data)}}))
                  )))
             ))
    ))
