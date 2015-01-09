(ns burgerboard-web.board
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [<! put! chan]]
            [burgerboard-web.widgets :as widgets]
            [burgerboard-web.api :as api]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(defn store-editor [data owner {:keys [stores-url]}]
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
      (dom/div #js {}
               (om/build widgets/save-single-value
                         data
                         {:opts {:className "store-editor"
                                 :k :name
                                 :value-saved (:new-value state)}})
               )
      )
    )
  )

(defn users-rating [user-email ratings]
  (some (fn [r] (if (= user-email (:user_email r))
                  (or (:rating r) "?")
                  nil))
        ratings))

(defn rating-value [selected-value]
  (if (= "?" selected-value)
    nil
    (int selected-value)
    ))

(defn store [data owner {:keys [stores-url user-email]}]
  (reify
    om/IInitState
    (init-state [this]
      {:new-value (chan)})
    om/IWillMount
    (will-mount [this]
      (go (while true
            (let [new-value (<! (om/get-state owner :new-value))
                  new-store (<! (api/json-put (:rating_url @data)
                                              {:rating (rating-value
                                                        new-value)}))]
              (om/transact! data (fn [_] new-store))
              )))
      )
    om/IRenderState
    (render-state [this state]
      (apply dom/li #js {:className "store"}
              (if-not (contains? data :id)
                (list (om/build store-editor data
                                {:opts {:stores-url stores-url}}))
                (list
                 (dom/span #js {:className "store-name"}
                           (:name data))
                 (dom/span #js {:className "rating-graph"})
                 (dom/span #js {:className "rating"}
                           (str (:rating data)))
                 (apply dom/select #js {:className "rating-option"
                                        :value (users-rating user-email
                                                             (:ratings data))
                                        :onChange #(put! (:new-value state)
                                                         (.. %
                                                             -target
                                                             -value))}
                        (cons
                         (dom/option #js {:value "?"} "?")
                         (map #(dom/option #js {:value %} %) (range 1 6))))
                 )
                )
              )
      )
    )
  )

(defn leaderboard [data owner {:keys [user-email]}]
  (reify
    om/IRender
    (render [this]
      (apply dom/ol #js {:className "leaderboard"}
             (let [created-stores (filter #(and
                                            (contains? % :id)
                                            (not (nil? (:rating %))))
                                          (:stores data))]
               (if (< (count created-stores) 2)
                 (list (dom/div #js {:className "store-teaser"}
                                "Add some more places!"))
                 (list
                  (om/build store
                            (apply max-key :rating created-stores)
                            {:opts {:user-email user-email}})
                  (om/build store
                            (apply min-key :rating created-stores)
                            {:opts {:user-email user-email}})
                  ))
               ))
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

(defn stores [data owner {:keys [stores-url user-email]}]
  (reify
    om/IRender
    (render [this]
      (apply dom/ol #js {:className "stores"}
             (concat
              (om/build-all store
                            (sort-by :rating descending data)
                            {:opts {:stores-url stores-url
                                    :user-email user-email}})
              (list (om/build add-store data))
             ))
      )
    )
  )

(defn board [data owner {:keys [user-email]}]
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
                   (om/build leaderboard data
                             {:opts {:user-email user-email}})
                   (om/build stores (:stores data)
                             {:opts {:stores-url (:stores_url data)
                                     :user-email user-email}}))
                  )))
             ))
    ))
