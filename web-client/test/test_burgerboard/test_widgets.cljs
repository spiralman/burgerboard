(ns test-burgerboard.test-widgets
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [cemerick.cljs.test
                    :refer (is deftest with-test testing test-var done)]
                   [huh.core :refer (with-rendered)]
                   )
  (:require
   [huh.core :as huh :refer [rendered tag containing with-class with-attr
                             sub-component with-text in
                             rendered-component setup-state after-event
                             get-rendered get-props]]
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
       (is (= "new value" (huh/get-state rendered-component :changing-value)))
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
       (tag "div"
            (with-class "value-editor")
            (containing
             (tag "label"
                  (with-class "value-editor-label")
                  (with-text "Label")
                  (containing
                   (tag "input"
                        (with-class "value-editor-input")
                        (with-attr "type" "text")
                        (with-attr "value" "Initial Value"))))
             (tag "button"
                  (with-class "value-editor-save")
                  (with-attr "type" "button")
                  (with-text ""))
             )
            )
       )
      )
  )

(deftest save-single-value-renders-error
  (is (rendered
       widgets/save-single-value
       {:value "Initial Value"}
       {:init-state {:error "error message"}
        :opts {:className "value-editor"
               :k :value
               :label "Label"}}
       (tag "div"
            (with-class "value-editor")
            (containing
             (tag "div"
                  (with-class "value-editor-error")
                  (with-text "error message"))
             (tag "label"
                  (with-class "value-editor-label")
                  (with-text "Label")
                  (containing
                   (tag "input"
                        (with-class "value-editor-input")
                        (with-attr "type" "text")
                        (with-attr "value" "Initial Value"))))
             (tag "button"
                  (with-class "value-editor-save")
                  (with-attr "type" "button")
                  (with-text ""))
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
                          :url "some/url"
                          :value-saved value-saved}})
        responded (expect-request
                   -test-ctx
                   {:method "POST"
                    :url "some/url"
                    :json-data {:value "Changed Value"}}
                   (json-response
                    201
                    {:id 1
                     :value "Changed Value"})
                   )]

    (after-event
     :change #js {:target #js {:value "Changed Value"}}
     (in rendered "input")
     (fn [_]
       (after-event
        :click #js {:target #js {}}
        (in rendered "button")
        (fn [_]
          (go
           (<! responded)
           (let [new-value (<! value-saved)]
             (is (= {:id 1
                     :value "Changed Value"}
                    new-value))
             (done)
             ))
          )
        )
       )
     )
    )
  )

(deftest ^:async save-single-value-handles-error-then-saves
  (let [state (setup-state {:user nil})
        value-saved (chan)
        rendered (rendered-component
                  widgets/save-single-value state
                  {:init-state {:temp-value "New Value"}
                   :opts {:className "value-editor"
                          :k :value
                          :url "some/url"
                          :value-saved value-saved}})
        responded (expect-request
                   -test-ctx
                   {:method "POST"
                    :url "some/url"
                    :json-data {:value "New Value"}}
                   {:status 400
                    :body "Error message"}
                   )]
    (after-event
     :click #js {}
     (in rendered "button")
     (fn [_]
       (go
        (<! responded)
        (is (= "Save failed"
               (huh/get-state rendered :error)))
        (let [second-response (expect-request
                               -test-ctx
                               {:method "POST"
                                :url "some/url"
                                :json-data {:value "New Value"}}
                               (json-response
                                201
                                {:id 1
                                 :value "New Value"})
                               )]
          (after-event
           :click #js {}
           (in rendered "button")
           (fn [_]
             (go
              (<! second-response)
              (is (= {:id 1
                      :value "New Value"}
                     (<! value-saved)))
              (done)
              )))
          )
        )
       ))
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
