(ns burgerboard-web.app
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! <! chan]]
            [burgerboard-web.widgets :as widgets]
            [burgerboard-web.group-nav :as group-nav]
            [burgerboard-web.board :as board]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [ajax.core :as ajax]))

(def initial-state
  (atom
   {:groups [{:id 1
              :name "Group"
              :boards_url "http://localhost/api/v1/groups/1/boards"
              :members_url "http://localhost/api/v1/groups/1/members"
              :boards [{:id 1
                        :name "Some Board"
                        :url "http://localhost/api/v1/groups/1/boards/1"
                        :stores_url
                        "http://localhost/api/v1/groups/1/boards/1/stores"
                        :group {:id 1 :name "Group"}
                        }
                       {:id 3
                        :name "New Board"
                        :url "http://localhost/api/v1/groups/1/boards/3"
                        :stores_url
                        "http://localhost/api/v1/groups/1/boards/3/stores"
                        :group {:id 1 :name "Group"}}]}
             {:id 2
              :name "Group2"
              :boards_url "http://localhost/api/v1/groups/2/boards"
              :members_url "http://localhost/api/v1/groups/2/members"}
             {:name "New Group"}]
    :board {:id 1 :name "Some Board"
            :group {:name "Group" :id 1}
            :url "http://localhost/api/v1/groups/1/boards/1"
            :stores_url "http://localhost/api/v1/groups/1/boards/1/stores"
            :stores [{:name "Store 1"
                      :id 1
                      :rating_url
                      "http://localhost/api/v1/groups/1/boards/1/stores/1/rating"
                      :rating 1.5
                      :ratings [{:user_email "owner@example.com"
                                 :rating 1}
                                {:user_email "some_user@example.com"
                                 :rating 2}]}
                     {:name "Store 2"
                      :id 2
                      :rating_url
                      "http://localhost/api/v1/groups/1/boards/1/stores/2/rating"
                      :rating 2.0
                      :ratings [{:user_email "owner@example.com"
                                 :rating 2}
                                {:user_email "some_user@example.com"
                                 :rating nil}]
                      }]}
    }
   )
  )

(defn json-post [url req]
  (let [response (chan)]
    (ajax/POST url
             {:params req
              :format :json
              :response-format :json
              :keywords? true
              :handler (fn [resp] (put! response resp))
              :error-handler (fn [err]
                               (.log js/console (str "got error " err)))}
             )
    response
    )
  )

(defn login! [resp {:keys [email password]}]
  (go (let [response (<! (json-post "/api/v1/login" {:email email
                                                    :password password}))]
        (put! resp response)))
  )

(defn login [data owner]
  (reify
    om/IInitState
    (init-state [this]
      {:email ""
       :password ""
       :on-login (chan)})
    om/IWillMount
    (will-mount [this]
      (let [on-login (om/get-state owner :on-login)]
        (go (let [login-response (<! on-login)]
              (om/transact! data (fn [_]
                                   {:user (dissoc login-response :groups)
                                    :groups (:groups login-response)
                                    :board nil}))
              ))
        )
      )
    om/IRenderState
    (render-state [this state]
      (dom/div #js {:className "login"}
               (om/build widgets/text-editor {}
                         {:opts {:state-owner owner
                                 :state-k :email
                                 :className "login-email"}})
               (om/build widgets/text-editor {}
                         {:opts {:state-owner owner
                                 :state-k :password
                                 :type "password"
                                 :className "login-password"}})
               (dom/button #js {:className "login-button"
                                :type "button"
                                :onClick (fn [] (login!
                                                 (om/get-state owner :on-login)
                                                 (om/get-state owner)))}
                           "Login")
               )
      )
    )
  )

(defn app [data owner]
  (reify
    om/IRender
    (render [this]
      (apply
       dom/div #js {:className "burgerboard"}
       (if (nil? (:user data))
         (list (om/build login data))
         (list
          (om/build group-nav/group-nav (:groups data))
          (om/build board/board (:board data))
          )
         )
       )
      )
    )
  )

(defn main []
  (om/root app initial-state
           {:target (. js/document (getElementById "burgerboard"))})
  )

(set! (.-onload js/window) main)
