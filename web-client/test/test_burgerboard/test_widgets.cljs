(ns test-burgerboard.test-widgets
  (:require-macros [cemerick.cljs.test
                    :refer (is deftest with-test testing test-var)]
                   [test-burgerboard.huh :refer (with-rendered)]
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
      {:changing-state "initial value"})
    om/IRenderState
    (render-state [this state]
      (dom/div #js {}
               (dom/input #js {:type "text"
                               :value (:changing-value data)
                               :onChange (widgets/bind-value
                                          owner :changing-value)})
               )
      )
    )
  )


(deftest bind-value-binds-to-state
  (let [rendered-component (rendered-component
                            test-editor (setup-state {}))]
    (after-event
     :change #js {:target #js {:value "new value"}}
     (in rendered-component
         "input")
     (fn [_]
       (is (= "new value" (om/get-state rendered-component :changing-value)))
       )
     )
    )
  )

(defn test-text-editor-parent [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:changing-state "initial value"})
    om/IRenderState
    (render-state [this state]
      (dom/div #js {}
               ;; Skip instrumentation, so we get a real text-editor
               ;; widget, not a stub
               (om/build* widgets/text-editor {}
                          {:opts {:state-k :changing-state
                                  :state-owner owner
                                  :className "test-editor"}})
               )
      )
    )
  )

(deftest text-editor-renders-input-element-for-value
  (is (rendered
       test-text-editor-parent {:value "Some Value"}
       (tag "div"
            (containing
             (tag "input"
                  (with-attr "type" "text")
                  (with-class "test-editor")
                  (with-attr "value" "initial value")
                  )
             )
            )
       )
      )
  )

(defn test-password-editor-parent [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:password "initial value"})
    om/IRenderState
    (render-state [this state]
      (dom/div #js {}
               ;; Skip instrumentation, so we get a real text-editor
               ;; widget, not a stub
               (om/build* widgets/text-editor {}
                          {:opts {:state-k :password
                                  :state-owner owner
                                  :type "password"
                                  :className "password-editor"}})
               )
      )
    )
  )

(deftest text-editor-renders-input-password-element-for-value
  (is (rendered
       test-password-editor-parent {}
       (tag "div"
            (containing
             (tag "input"
                  (with-attr "type" "password")
                  (with-class "password-editor")
                  (with-attr "value" "initial value")
                  )
             )
            )
       )
      )
  )

(deftest text-editor-binds-name-to-cursor
  (let [rendered (rendered-component
                  test-text-editor-parent (setup-state {}))]
    (after-event
     :change #js {:target #js {:value "New Value"}}
     (in rendered "input")
     (fn [_]
       (is (= "New Value" (om/get-state rendered :changing-state)))
       )
     )
    )
  )

(deftest save-single-value-renders-controls
  (is (rendered
       widgets/save-single-value
       {:value "Initial Value"}
       {:opts {:className "value-editor"
               :k :value}}
       (with-rendered [component]
         (tag "div"
              (with-class "value-editor")
              (containing
               (sub-component widgets/text-editor {}
                              {:opts {:state-k :temp-value
                                      :state-owner component
                                      :className "value-editor-input"}})
               (tag "button"
                    (with-class "value-editor-save")
                    (with-attr "type" "button")
                    (with-text "Save"))
               )
              )
         )
       )
      )
  )

(deftest save-single-value-updates-cursor-on-save
  (let [app-state (setup-state {:value "Initial Value"})
        rendered (rendered-component
                  widgets/save-single-value
                  app-state
                  {:opts {:className "value-editor"
                          :k :value}})]
    (after-event
     :change #js {:target #js {:value "Changed Value"}}
     (in rendered "input")
     (fn [_]
       (after-event
        :click #js {:target #js {}}
        (in rendered "button")
        (fn [_]
          (is (= "Changed Value" (:value @app-state)))
          )
        )
       )
     )
    )
  )
