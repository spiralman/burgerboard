(ns burgerboard-test.test-app
  (:require-macros [cemerick.cljs.test
                    :refer (is deftest with-test testing test-var)]
                   )
  (:require [cemerick.cljs.test :as t]
            [burgerboard-web.app :as app]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [clojure.string :refer [upper-case]]
            )
  )

(defn -instrument [sub-component cursor _]
  {:sub-component sub-component
   :cursor cursor
   }
  )

(defn rendered [component state & tests]
  (binding [om/*instrument* -instrument]
    (let [rendered-comp (.render (om/build* component state {}))]
      ((apply every-pred tests) rendered-comp)
      )
    )
  )

(defn tag [tag-name & tests]
  (fn [component]
    (and
     (= (upper-case tag-name) (.-tagName component))
     (if (empty? tests)
       true
       ((apply every-pred tests) component)
       )
     )
    )
  )

(defn containing [& tests]
  (fn [component]
    (let [children (js->clj (.. component -props -children))]
      (if (sequential? children)
        (and
         (= (count tests) (count children))
         (every? true?
                 (map (fn [pred child] (pred child))
                      tests children
                      )
                 )
         )
        (and
         (= 1 (count tests))
         ((nth tests 0) children) ;; Children is actually just one child
         )
        )
      )
    )
  )

(defn text [text]
  (fn [component]
    (= text component)
    )
  )

(defn sub-component [sub-component cursor]
  (fn [component]
    (= {:sub-component sub-component
        :cursor cursor}
       component
       )
    )
  )

(defn with-class [class-name]
  (fn [component]
    (= class-name (.. component -props -className))
    )
  )

(deftest app-contains-sub-components
  (is (rendered
       app/app {:groups [{:id 1}]
                :boards [{:id 1}]}
       (tag "div"
            (with-class "burgerboard")
            (containing
             (sub-component app/group-nav [{:id 1}])
             (sub-component app/board [{:id 1}])
             )
            )
       )
      )
  )

(deftest group-nav-contains-groups
  (is (rendered
       app/group-nav [{:id 1}
                      {:id 2}]
       (tag "ul"
            (with-class "groups")
            (containing
             (sub-component app/group {:id 1})
             (sub-component app/group {:id 2})
             )
            )
       )
      )
  )

(deftest group-contains-board-nav
  (is (rendered
       app/group {:id 1
                  :name "Some Group"
                  :boards [{:id 1} {:id 2}]
                  }
       (tag "li"
            (with-class "group")
            (containing
             (tag "span"
                  (with-class "group-name")
                  (containing
                   (text "Some Group")
                   )
                  )
             (tag "ul"
                  (with-class "boards")
                  (containing
                   (sub-component app/board-nav {:id 1})
                   (sub-component app/board-nav {:id 2})
                   )
                  )
             )
            )
       )
      )
  )
