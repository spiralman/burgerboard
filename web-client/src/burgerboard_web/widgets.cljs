(ns burgerboard-web.widgets
  (:require [om.dom :as dom :include-macros true]
            [om.core :as om :include-macros true]))

(defn loading []
  (dom/div #js {:className "loading"})
  )

(defn bind-value [owner k]
  (fn -bind-value [event]
    (om/set-state! owner k (.. event -target -value))
    )
  )

(defn text-editor [data owner {:keys [state-k state-owner className type]}]
  (reify
    om/IRender
    (render [_]
      (dom/input #js {:type (or type "text")
                      :value (om/get-state state-owner state-k)
                      :className className
                      :onChange (bind-value state-owner state-k)}))
    ))

(defn save-single-value [data owner {:keys [className k]}]
  (reify
    om/IInitState
    (init-state [this]
      {:temp-value (k data)})
    om/IRenderState
    (render-state [this state]
      (dom/div #js {:className className}
               (om/build text-editor {}
                         {:opts {:state-k :temp-value
                                 :state-owner owner
                                 :className (str className "-input")}})
               (dom/button #js {:className (str className "-save")
                                :type "button"
                                :onClick (fn [_] (om/transact!
                                                  data k
                                                  (fn [_]
                                                    (om/get-state
                                                     owner :temp-value))))}
                          "Save")
               )
      )
    )
  )
