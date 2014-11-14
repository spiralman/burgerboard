(ns test-burgerboard.test-widgets
  (:require-macros [cemerick.cljs.test
                    :refer (is deftest with-test testing test-var)]
                   [test-burgerboard.huh]
                   )
  (:require
   [test-burgerboard.huh :refer [rendered tag containing with-class with-attr
                                 sub-component text nothing in
                                 rendered-component setup-state after-event]]
   [burgerboard-web.widgets :as widgets]
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]
   )
  )

(defn test-editor [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/div #js {}
               (dom/input #js {:type "text"
                               :value (:changing-value data)
                               :onChange (widgets/bind-value
                                          data :changing-value)})
               )
      )
    )
  )


(deftest bind-value-binds-to-cursor
  (let [state (setup-state {:changing-value "initial value"})]
    (after-event
     :onChange #js {:target #js {:value "new value"}}
     (in (rendered-component
          test-editor state)
         0)
     (fn [_]
       (is (= "new value" (:changing-value @state)))
       )
     )
    )
  )

(deftest text-editor-renders-input-element-for-value
  (is (rendered
       widgets/text-editor {:value "Some Value"}
       {:opts {:attr :value :class "some-editor"}}
       (tag "input"
            (with-attr "type" "text")
            (with-class "some-editor")
            (with-attr "value" "Some Value")
            (containing nothing)
            )
       )
      )
  )

(deftest text-editor-binds-name-to-cursor
  (let [state (setup-state {:value ""})]
    (after-event
     :onChange #js {:target #js {:value "New Value"}}
     (rendered-component
      widgets/text-editor state
      {:opts {:attr :value :class "some-class"}})
     (fn [_]
       (is (= "New Value" (:value @state)))
       )
     )
    )
  )
