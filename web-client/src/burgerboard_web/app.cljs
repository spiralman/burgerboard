(ns burgerboard-web.app
  (:require-macros [cljs.core.async.macros :refer [go]])
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

(defn login! [resp {:keys [email password]}]
  (go (let [response (<! (api/json-post "/api/v1/login" {:email email
                                                         :password password}))]
        (put! resp response)))
  )

(defn signup! [resp {:keys [name email password]}]
  (go (let [response (<! (api/json-post "/api/v1/signups" {:name name
                                                       :email email
                                                       :password password}))]
        (put! resp response)))
  )

(defn parse-login [login-response]
  {:user (dissoc login-response :groups)
   :groups (:groups login-response)
   :board nil}
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
                                   (parse-login login-response)))
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

(defn signup [data owner]
  (reify
    om/IInitState
    (init-state [this]
      {:email ""
       :name ""
       :password ""
       :on-signup (chan)})
    om/IWillMount
    (will-mount [this]
      (let [on-signup (om/get-state owner :on-signup)]
        (go (let [signup-response (<! on-signup)]
              (om/transact! data (fn [_]
                                   {:user (dissoc signup-response :groups)
                                    :groups (:groups signup-response)
                                    :board nil}))
              ))
        )
      )
    om/IRenderState
    (render-state [this state]
      (dom/div #js {:className "signup"}
               (om/build widgets/text-editor {}
                         {:opts {:state-owner owner
                                 :state-k :name
                                 :className "signup-name"}})
               (om/build widgets/text-editor {}
                         {:opts {:state-owner owner
                                 :state-k :email
                                 :className "signup-email"}})
               (om/build widgets/text-editor {}
                         {:opts {:state-owner owner
                                 :state-k :password
                                 :type "password"
                                 :className "signup-password"}})
               (dom/button #js {:className "signup-button"
                                :type "button"
                                :onClick (fn [] (signup!
                                                 (om/get-state owner :on-signup)
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

(defn app [data owner]
  (reify
    om/IRender
    (render [this]
      (apply
       dom/div #js {:className "burgerboard"}
       (if (nil? (:user data))
         (list (om/build connect data))
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
  (if-let [user-state (.-burgerboard_init_state js/window)]
    (reset! initial-state (parse-login (js->clj user-state
                                                :keywordize-keys true)))
    )
  (om/root app initial-state
           {:target (. js/document (getElementById "burgerboard"))})
  )

(set! (.-onload js/window) main)
