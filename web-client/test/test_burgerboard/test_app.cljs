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
               (tag "h2"
                    (with-class "login-title")
                    (with-text "Login"))
               (sub-component widgets/text-editor {}
                              {:opts {:state-owner login
                                      :state-k :email
                                      :label "Email"
                                      :className "login-email"}})
               (sub-component widgets/text-editor {}
                              {:opts {:state-owner login
                                      :state-k :password
                                      :label "Password"
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

(deftest login-renders-error
  (is (rendered
       app/login {:user nil}
       {:init-state {:error "Some error"}}
       (with-rendered [login]
         (tag "div"
              (with-class "login")
              (containing
               (tag "h2"
                    (with-class "login-title")
                    (with-text "Login"))
               (tag "div"
                    (with-class "login-error")
                    (with-text "Some error"))
               (sub-component widgets/text-editor {}
                              {:opts {:state-owner login
                                      :state-k :email
                                      :label "Email"
                                      :className "login-email"}})
               (sub-component widgets/text-editor {}
                              {:opts {:state-owner login
                                      :state-k :password
                                      :label "Password"
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

(deftest ^:async login-tells-user-on-error
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
                   {:status 403
                    :body "Invalid username or password"}
                   )]
    (after-event
     :click #js {}
     (in login "button")
     (fn [_]
       (go
        (<! responded)
        (is (= {:user nil}
               @state))
        (is (= "Could not log in" (om/get-state login :error)))
        (done)
        )
       )
     )
    )
  )

(deftest ^:async login-logs-in-after-error
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
                   {:status 403
                    :body "Invalid username or password"}
                   )]
    (after-event
     :click #js {}
     (in login "button")
     (fn [_]
       (go
        (<! responded)
        (let [second-response (expect-request
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
              (<! second-response)
              (is (= {:user {:email "some_user@example.com"
                             :groups_url "http://localhost/api/v1/groups"
                             :name "Some User"}
                      :groups [{:name "Group" :id 1}]
                      :board nil}
                     @state))
              (done)
              )))
          )
        )
       ))
    )
  )

(deftest signup-contains-signup-controls
  (is (rendered
       app/signup {:user nil}
       (with-rendered [signup]
         (tag "div"
              (with-class "signup")
              (containing
               (tag "h2"
                    (with-class "signup-title")
                    (with-text "Signup"))
               (sub-component widgets/text-editor {}
                              {:opts {:state-owner signup
                                      :state-k :name
                                      :label "Name"
                                      :className "signup-name"}})
               (sub-component widgets/text-editor {}
                              {:opts {:state-owner signup
                                      :state-k :email
                                      :label "Email"
                                      :className "signup-email"}})
               (sub-component widgets/text-editor {}
                              {:opts {:state-owner signup
                                      :state-k :password
                                      :label "Password"
                                      :type "password"
                                      :className "signup-password"}})
               (tag "button"
                    (with-class "signup-button")
                    (with-attr "type" "button")
                    (with-text "Signup"))
               )
              )
         )
       )
      )
  )

(deftest signup-displays-error
  (is (rendered
       app/signup {:user nil}
       {:init-state {:error "Error message"}}
       (with-rendered [signup]
         (tag "div"
              (with-class "signup")
              (containing
               (tag "h2"
                    (with-class "signup-title")
                    (with-text "Signup"))
               (tag "div"
                    (with-class "signup-error")
                    (with-text "Error message"))
               (sub-component widgets/text-editor {}
                              {:opts {:state-owner signup
                                      :state-k :name
                                      :label "Name"
                                      :className "signup-name"}})
               (sub-component widgets/text-editor {}
                              {:opts {:state-owner signup
                                      :state-k :email
                                      :label "Email"
                                      :className "signup-email"}})
               (sub-component widgets/text-editor {}
                              {:opts {:state-owner signup
                                      :state-k :password
                                      :label "Password"
                                      :type "password"
                                      :className "signup-password"}})
               (tag "button"
                    (with-class "signup-button")
                    (with-attr "type" "button")
                    (with-text "Signup"))
               )
              )
         )
       )
      )
  )

(deftest ^:async signup-signs-up-on-click
  (let [state (setup-state {:user nil})
        signup (rendered-component
                app/signup state
                {:init-state {:name "New User"
                              :email "some_user@example.com"
                              :password "password"}})
        responded (expect-request
                   -test-ctx
                   {:method "POST"
                    :url "/api/v1/signups"
                    :json-data {:name "New User"
                                :email "some_user@example.com"
                                :password "password"}}
                   (json-response
                    201
                    {:email "some_user@example.com"
                     :name "New User"
                     :groups_url "http://localhost/api/v1/groups"
                     :groups []})
                   )]
    (after-event
     :click #js {}
     (in signup "button")
     (fn [_]
       (go
        (<! responded)
        (is (= {:user {:email "some_user@example.com"
                       :groups_url "http://localhost/api/v1/groups"
                       :name "New User"}
                :groups []
                :board nil}
               @state))
        (done)
        )
       )
     )
    )
  )

(deftest ^:async signup-notifies-user-on-error
  (let [state (setup-state {:user nil})
        signup (rendered-component
                app/signup state
                {:init-state {:name "New User"
                              :email "some_user@example.com"
                              :password "password"}})
        responded (expect-request
                   -test-ctx
                   {:method "POST"
                    :url "/api/v1/signups"
                    :json-data {:name "New User"
                                :email "some_user@example.com"
                                :password "password"}}
                   {:status 400
                    :body "Invalid user"}
                   )]
    (after-event
     :click #js {}
     (in signup "button")
     (fn [_]
       (go
        (<! responded)
        (is (= {:user nil}
               @state))
        (is (= "Could not sign up"
               (om/get-state signup :error)))
        (done)
        )
       )
     )
    )
  )

(deftest ^:async signup-signs-up-after-error
  (let [state (setup-state {:user nil})
        signup (rendered-component
                app/signup state
                {:init-state {:name "New User"
                              :email "some_user@example.com"
                              :password "password"}})
        responded (expect-request
                   -test-ctx
                   {:method "POST"
                    :url "/api/v1/signups"
                    :json-data {:name "New User"
                                :email "some_user@example.com"
                                :password "password"}}
                   {:status 400
                    :body "Invalid user"}
                   )]
    (after-event
     :click #js {}
     (in signup "button")
     (fn [_]
       (go
        (<! responded)
        (let [second-response (expect-request
                               -test-ctx
                               {:method "POST"
                                :url "/api/v1/signups"
                                :json-data {:name "New User"
                                            :email "some_user@example.com"
                                            :password "password"}}
                               (json-response
                                201
                                {:email "some_user@example.com"
                                 :name "New User"
                                 :groups_url "http://localhost/api/v1/groups"
                                 :groups []})
                               )]
          (after-event
           :click #js {}
           (in signup "button")
           (fn [_]
             (go
              (<! second-response)
              (is (= {:user {:email "some_user@example.com"
                             :groups_url "http://localhost/api/v1/groups"
                             :name "New User"}
                      :groups []
                      :board nil}
                     @state))
              (done)
              )))
          )
        )
       ))
    )
  )

(deftest connect-contains-login-and-signup
  (is (rendered
       app/connect {:user nil}
       (tag "div"
            (with-class "connect")
            (containing
             (sub-component app/signup {:user nil})
             (sub-component app/login {:user nil}))
            )
       )
      )
  )

(deftest header-contains=logo
  (is (rendered
       app/header {}
       (tag "div"
            (with-class "header")
            (containing
             (tag "div"
                  (with-class "header-nav")
                  (containing
                   (tag "h1"
                        (with-class "logo")
                        (with-text "Burgerboard"))
                   ))
             ))
       ))
  )

(deftest app-contains-login-without-user
  (is (rendered
       app/app {:user nil}
       (tag "div"
            (with-class "burgerboard")
            (containing
             (sub-component app/header {})
             (tag "div"
                  (with-class "content")
                  (containing
                   (sub-component app/connect {:user nil}))
                  )
             ))
       )
      )
  )

(deftest app-contains-sub-components
  (is (rendered
       app/app {:user {:id 1 :email "user@email.com"}
                :groups [{:id 1}]
                :board {:id 1}}
       (with-rendered [app-comp]
         (tag "div"
              (with-class "burgerboard")
              (containing
               (sub-component app/header {})
               (tag "div"
                    (with-class "content")
                    (containing
                      (sub-component group-nav/group-nav [{:id 1}]
                                     {:opts
                                      {:select-board (om/get-state app-comp
                                                                   :select-board)}})
                      (sub-component board/board {:id 1}
                                     {:opts {:user-email "user@email.com"}})
                      ))
               )
              )
         )
       )
      )
  )

(deftest ^:async app-updates-board-when-selected
  (let [state (setup-state {:user {:id 1}
                            :groups []
                            :board nil})
        app-comp (rendered-component app/app state)
        select-board (om/get-state app-comp :select-board)]
    (put! select-board {:id 1 :name "Selected Board"}
          (fn [_]
            (is (= {:id 1 :name "Selected Board"}
                   (:board @state)))
            (done)
            ))
    ))
