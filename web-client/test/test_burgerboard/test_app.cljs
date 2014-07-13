(ns burgerboard-test.test-app
  (:require-macros [cemerick.cljs.test
                    :refer (is deftest with-test run-test testing test-var)]
                   )
  (:require [cemerick.cljs.test :as t]
            [burgerboard-web.app :as app]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [clojure.string :refer [upper-case]]
            )
  )

(defn rendered [component state & tests]
  (let [rendered-comp (.render (om/build component state {}))]
    ((apply every-pred tests) rendered-comp)
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
    (every? true?
     (map (fn [pred child] (pred child))
          tests
          (js->clj (.. component -props -children))
          )
     )
    )
  )

(defn text [text]
  (fn [component]
    (= text component)
    )
  )

(defn with-class [class-name]
  (fn [component]
    (= class-name (.. component -props -className))
    )
  )

(deftest hello-test
  (is (rendered
       app/app {:text "the text"}
       (tag "h1"
            (with-class "app")
            (containing
             (text "the text")
             (tag "span")
             (tag "span"))
            )
       )
      )
  )
