(ns test-burgerboard.test-app
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [cemerick.cljs.test
                    :refer (is deftest with-test testing test-var done)]
                   [test-burgerboard.huh :refer (with-rendered)]
                   )
  (:require
   [cljs.core.async :refer [<! chan put!]]
   [test-burgerboard.huh :refer [rendered tag containing with-class sub-component with-attr with-text setup-state rendered-component after-event in]]
   [test-burgerboard.fake-server :refer [expect-request json-response]]
   [burgerboard-web.widgets :as widgets]
   [burgerboard-web.app :as app]
   [burgerboard-web.group-nav :as group-nav]
   [burgerboard-web.board :as board]
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]
   )
  )

(deftest login-contains-login-controls
  (is (rendered
       app/login {:user nil}
       (with-rendered [login]
         (tag "div"
              (with-class "login")
              (containing
               (sub-component widgets/text-editor {}
                              {:opts {:state-owner login
                                      :state-k :email
                                      :className "login-email"}})
               (sub-component widgets/text-editor {}
                              {:opts {:state-owner login
                                      :state-k :password
                                      :type "password"
                                      :className "login-password"}})
               (tag "button"
                    (with-class "login-button")
                    (with-attr "type" "button")
                    (with-text "Login"))
               )
              )
         )
       )
      )
  )

(deftest ^:async login-logs-in-on-click
  (let [state (setup-state {:user nil})
        login (rendered-component
               app/login state
               {:init-state {:email "email"
                             :password "password"}})
        responded (expect-request
                   -test-ctx
                   {:method "POST"
                    :url "/api/v1/login"
                    :json-data {:email "email"
                                :password "password"}}
                   (json-response
                    200
                    {:email "some_user@example.com"
                     :name "Some User"
                     :groups_url "http://localhost/api/v1/groups"
                     :groups [{:name "Group" :id 1}]})
                   )]
    (after-event
     :click #js {}
     (in login "button")
     (fn [_]
       (go
        (<! responded)
        (is (= {:user {:email "some_user@example.com"
                       :groups_url "http://localhost/api/v1/groups"
                       :name "Some User"}
                :groups [{:name "Group" :id 1}]
                :board nil}
               @state))
        (done)
        )
       )
     )
    )
  )

(deftest app-contains-login-without-user
  (is (rendered
       app/app {:user nil}
       (tag "div"
            (with-class "burgerboard")
            (containing
             (sub-component app/login {:user nil}))
            )
       )
      )
  )

(deftest app-contains-sub-components
  (is (rendered
       app/app {:user {:id 1}
                :groups [{:id 1}]
                :board {:id 1}}
       (tag "div"
            (with-class "burgerboard")
            (containing
             (sub-component group-nav/group-nav [{:id 1}])
             (sub-component board/board {:id 1})
             )
            )
       )
      )
  )
