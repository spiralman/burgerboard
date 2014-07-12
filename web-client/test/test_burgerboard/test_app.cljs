(ns burgerboard-test.test-app
  (:require-macros [cemerick.cljs.test
                    :refer (is deftest with-test run-test testing test-var)]
                   )
  (:require [cemerick.cljs.test :as t]
            [burgerboard-web.app :as app]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
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
     (= tag-name (.-tagName component))
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

(deftest hello-test
  (is (rendered
       app/app {:text "the text"}
       (tag "H1"
            (containing
             (text "the text")
             (tag "SPAN")
             (tag "SPAN"))
            )
       )
      )
  )

  ;;  (let [a (om/build app/app {} {})
  ;;        rendered (.render a)
  ;;        ]
  ;;    (tag rendered "H1")
  ;;    )
  ;;  )
  ;; )
