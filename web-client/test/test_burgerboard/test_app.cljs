(ns test-burgerboard.test-app
  (:require-macros [cemerick.cljs.test
                    :refer (is deftest with-test testing test-var)]
                   [test-burgerboard.huh :refer (with-rendered)]
                   )
  (:require
   [test-burgerboard.huh :refer [rendered tag containing with-class sub-component with-attr with-text]]
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
