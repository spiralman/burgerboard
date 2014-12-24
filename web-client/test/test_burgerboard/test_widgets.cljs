(ns test-burgerboard.test-widgets
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [cemerick.cljs.test
                    :refer (is deftest with-test testing test-var done)]
                   [test-burgerboard.huh :refer (with-rendered)]
                   )
  (:require
   [test-burgerboard.huh :refer [rendered tag containing with-class with-attr
                                 sub-component with-text in
                                 rendered-component setup-state after-event]]
   [test-burgerboard.fake-server :refer [expect-request json-response]]
   [burgerboard-web.widgets :as widgets]
   [cljs.core.async :refer [put! <! chan]]
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
                                  :className "test-editor"
                                  :label "Label"}})
               )
      )
    )
  )

(deftest text-editor-renders-input-element-for-value
  (is (rendered
       test-text-editor-parent {:value "Some Value"}
       (tag "div"
            (containing
             (tag "label"
                  (with-class "test-editor-label")
                  (with-text "Label")
                  (containing
                   (tag "input"
                        (with-attr "type" "text")
                        (with-class "test-editor-input")
                        (with-attr "value" "initial value")
                        )
                   ))
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
                                  :label "Label"
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
             (tag "label"
                  (with-class "password-editor-label")
                  (with-text "Label")
                  (containing
                   (tag "input"
                        (with-attr "type" "password")
                        (with-class "password-editor-input")
                        (with-attr "value" "initial value")
                        )
                   ))
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
               :k :value
               :label "Label"}}
       (with-rendered [component]
         (tag "div"
              (with-class "value-editor")
              (containing
               (sub-component widgets/text-editor {}
                              {:opts {:state-k :temp-value
                                      :state-owner component
                                      :label "Label"
                                      :className "value-editor"}})
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

(deftest ^:async save-single-value-updates-channel-on-save
  (let [app-state (setup-state {:value "Initial Value"})
        value-saved (chan)
        rendered (rendered-component
                  widgets/save-single-value
                  app-state
                  {:opts {:className "value-editor"
                          :k :value
                          :value-saved value-saved}})]
    (after-event
     :change #js {:target #js {:value "Changed Value"}}
     (in rendered "input")
     (fn [_]
       (after-event
        :click #js {:target #js {}}
        (in rendered "button")
        (fn [_]
          (go (let [new-value (<! value-saved)]
                (is (= "Changed Value" new-value))
                (done)
                ))
          )
        )
       )
     )
    )
  )

(deftest ^:async loader-renders-loading-div
  (let [responded (expect-request
                   -test-ctx
                   {:method "GET"
                    :url "http://resource"}
                   (json-response
                    201
                    {:some "data"})
                   )]

    (is (rendered
         widgets/loader
         {:resource-url "http://resource"}
         {:opts {:load-from :resource-url
                 :load-into :resource}}
         (tag "div"
              (with-class "loading"))
         ))
    (go (<! responded)
        (done))
    )
  )

(deftest ^:async loader-fetches-data-and-inserts-into-state
  (let [state (setup-state {:resource-url "http://resource"})
        loader (rendered-component
                widgets/loader state
                {:opts {:load-from :resource-url
                        :load-into :resource}})
        responded (expect-request
                   -test-ctx
                   {:method "GET"
                    :url "http://resource"}
                   (json-response
                    201
                    {:some "data"})
                   )]
    (go
     (<! responded)
     (is (= {:resource-url "http://resource"
             :resource {:some "data"}}
            @state))
     (done)
     )
    )
  )

(deftest ^:async loader-extracts-from-response
  (let [state (setup-state {:resource-url "http://resource"})
        loader (rendered-component
                widgets/loader state
                {:opts {:load-from :resource-url
                        :load-into :resource
                        :load-keys [:data]}})
        responded (expect-request
                   -test-ctx
                   {:method "GET"
                    :url "http://resource"}
                   (json-response
                    201
                    {:data {:some "data"}})
                   )]
    (go
     (<! responded)
     (is (= {:resource-url "http://resource"
             :resource {:some "data"}}
            @state))
     (done)
     )
    )
  )
