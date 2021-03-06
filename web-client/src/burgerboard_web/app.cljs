(ns burgerboard-web.app
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]])
  (:require [cljs.core.async :refer [put! <! chan]]
            [burgerboard-web.widgets :as widgets]
            [burgerboard-web.group-nav :as group-nav]
            [burgerboard-web.board :as board]
            [burgerboard-web.api :as api]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(def initial-state
  (atom
   {:user nil
    :groups nil
    :board nil
    }
   )
  )

(defn parse-login [login-response]
  {:user (dissoc login-response :groups)
   :groups (:groups login-response)
   :board nil}
  )

(defn login! [resp err {:keys [email password]}]
  (let [[post-response post-error] (api/json-post "/api/v1/login"
                                                  {:email email
                                                   :password password})]
    (go (alt!
         post-response ([response] (put! resp (parse-login response)))
         post-error (put! err "Could not log in")
         ))
    ))

(defn signup! [resp err {:keys [name email password]}]
  (let [[post-response post-error] (api/json-post "/api/v1/signups"
                                                 {:name name
                                                  :email email
                                                  :password password})]
    (go (alt!
         post-response ([response] (put! resp response))
         post-error (put! err "Could not sign up")
         ))
    ))

(defn logout! [resp]
  (go (<! (api/delete "/api/v1/login/current"))
      (put! resp "")))

(defn login [data owner]
  (reify
    om/IInitState
    (init-state [this]
      {:email ""
       :password ""
       :on-login (chan)
       :on-error (chan)})
    om/IWillMount
    (will-mount [this]
      (let [on-login (om/get-state owner :on-login)
            on-error (om/get-state owner :on-error)]
        (go-loop []
                 (alt!
                  on-login ([login-response] (om/transact!
                                              data (fn [_] login-response)))
                  on-error ([error] (do
                                      (om/set-state! owner :error error)
                                      (recur)))
             ))
        ))
    om/IRenderState
    (render-state [this state]
      (dom/div #js {:className "login"}
               (dom/h2 #js {:className "login-title"}
                       "Login")
               (if (contains? (om/get-state owner) :error)
                 (dom/div #js {:className "login-error"}
                          (om/get-state owner :error)))
               (widgets/text-editor {:state-owner owner
                                     :state-k :email
                                     :label "Email"
                                     :className "login-email"})
               (widgets/text-editor {:state-owner owner
                                     :state-k :password
                                     :label "Password"
                                     :type "password"
                                     :className "login-password"})
               (dom/button #js {:className "login-button"
                                :type "button"
                                :onClick (fn [] (login!
                                                 (om/get-state owner :on-login)
                                                 (om/get-state owner :on-error)
                                                 (om/get-state owner)))}
                           "Login")
               )
      )
    )
  )

(defn signup [data owner]
  (reify
    om/IInitState
    (init-state [this]
      {:email ""
       :name ""
       :password ""
       :on-signup (chan)
       :on-error (chan)})
    om/IWillMount
    (will-mount [this]
      (let [on-signup (om/get-state owner :on-signup)
            on-error (om/get-state owner :on-error)]
        (go-loop []
                 (alt!
                  on-signup ([signup-response]
                               (om/transact! data (fn [_]
                                                    {:user (dissoc signup-response
                                                                   :groups)
                                                     :groups (:groups signup-response)
                                                     :board nil})))
                  on-error ([error] (do
                                      (om/set-state! owner :error error)
                                      (recur)))
                  ))
        ))
    om/IRenderState
    (render-state [this state]
      (dom/div #js {:className "signup"}
               (dom/h2 #js {:className "signup-title"}
                       "Signup")
               (if (contains? (om/get-state owner) :error)
                 (dom/div #js {:className "signup-error"}
                          (om/get-state owner :error)))
               (widgets/text-editor {:state-owner owner
                                     :state-k :name
                                     :label "Name"
                                     :className "signup-name"})
               (widgets/text-editor {:state-owner owner
                                     :state-k :email
                                     :label "Email"
                                     :className "signup-email"})
               (widgets/text-editor {:state-owner owner
                                     :state-k :password
                                     :label "Password"
                                     :type "password"
                                     :className "signup-password"})
               (dom/button #js {:className "signup-button"
                                :type "button"
                                :onClick (fn [] (signup!
                                                 (om/get-state owner :on-signup)
                                                 (om/get-state owner :on-error)
                                                 (om/get-state owner)))}
                           "Signup")
               )
      )
    )
  )

(defn connect [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/div #js {:className "connect"}
               (om/build signup data)
               (om/build login data)
               )
      )
    )
  )

(defn logout [data owner]
  (reify
    om/IInitState
    (init-state [this]
      {:on-logout (chan)})
    om/IWillMount
    (will-mount [this]
      (go (<! (om/get-state owner :on-logout))
          (om/transact! data (fn [_] {:user nil
                                      :groups nil
                                      :board nil}))
          ))
    om/IRenderState
    (render-state [this state]
      (dom/a #js {:className "logout"
                  :href "#"
                  :onClick #(logout! (:on-logout state))}
             "Logout")
      )
    ))

(defn header [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/div #js {:className "header"}
               (dom/div #js {:className "header-nav"}
                        (dom/h1 #js {:className "logo"} "Burgerboard")
                        (if (:user data)
                          (om/build logout data))
               ))
      )
    ))

(defn app [data owner]
  (reify
    om/IInitState
    (init-state [this]
      {:select-board (chan)})
    om/IWillMount
    (will-mount [this]
      (go (while true
            (let [new-board (<! (om/get-state owner :select-board))]
              (om/transact! data :board (fn [_] new-board))
              )))
      )
    om/IRenderState
    (render-state [this state]
      (dom/div
       #js {:className "burgerboard"}
       (om/build header data)
       (apply dom/div #js {:className "content"}
              (if (nil? (:user data))
                (list (om/build connect data))
                (list
                 (om/build group-nav/group-nav (:groups data)
                           {:opts {:select-board (:select-board state)}})
                 (om/build board/board (:board data)
                           {:opts {:user-email (:email (:user data))}})
                 )
                )
              )
       )
      )
    )
  )

(defn main []
  (if-let [user-state (.-burgerboard_init_state js/window)]
    (reset! initial-state (parse-login (js->clj user-state
                                                :keywordize-keys true)))
    )
  (om/root app initial-state
           {:target (. js/document (getElementById "burgerboard"))})
  )

(set! (.-onload js/window) main)
