(ns test-burgerboard.test-app
  (:require-macros [cemerick.cljs.test
                    :refer (is deftest with-test testing test-var)]
                   [test-burgerboard.huh]
                   )
  (:require
   [test-burgerboard.huh :refer [rendered tag containing with-class sub-component with-text]]
   [burgerboard-web.app :as app]
   [burgerboard-web.group-nav :as group-nav]
   [burgerboard-web.board :as board]
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]
   )
  )

;; (deftest login-contains-login-controls
;;   (is (rendered
;;        app/login {:user nil}
;;        (tag "div"
;;             (with-class "login")
;;             (containing


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
