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
     ((apply every-pred tests) component)
     )
    )
  )

(defn containing [& tests]
  (fn [component]
    ((apply some-fn tests) (.. -component -props -children))
    )
  )

(deftest hello-test
  (is (rendered
       app/app {}
       (tag "DIV"
            (containing
             (tag "SPAN"))
            (containing
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
