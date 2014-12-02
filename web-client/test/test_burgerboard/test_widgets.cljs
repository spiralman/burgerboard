(ns test-burgerboard.test-widgets
  (:require-macros [cemerick.cljs.test
                    :refer (is deftest with-test testing test-var)]
                   [test-burgerboard.huh]
                   )
  (:require
   [test-burgerboard.huh :refer [rendered tag containing with-class with-attr
                                 sub-component with-text in
                                 rendered-component setup-state after-event]]
   [burgerboard-web.widgets :as widgets]
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]
   )
  )

(defn test-editor [data owner]
  (reify
    om/IInitState
    (init-state [_]
      (.log js/console "returning initial state")
      {:changing-state "initial value"})
    om/IRenderState
    (render-state [this state]
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
     :change #js {:target #js {:value "new value"}}
     (in (rendered-component
          test-editor state)
         "input")
     (fn [_]
       (is (= "new value" (:changing-value @state)))
       )
     )
    )
  )

(deftest text-editor-renders-input-element-for-value
  (is (rendered
       widgets/text-editor {:value "Some Value"}
       {:opts {:attr :value :className "some-editor"}}
       (tag "input"
            (with-attr "type" "text")
            (with-class "some-editor")
            (with-attr "value" "Some Value")
            )
       )
      )
  )

(deftest text-editor-binds-name-to-cursor
  (let [state (setup-state {:value ""})]
    (after-event
     :change #js {:target #js {:value "New Value"}}
     (in (rendered-component
          widgets/text-editor state
          {:opts {:attr :value :className "some-class"}})
         "")
     (fn [_]
       (is (= "New Value" (:value @state)))
       )
     )
    )
  )
