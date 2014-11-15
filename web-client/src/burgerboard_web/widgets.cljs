(ns burgerboard-web.widgets
  (:require [om.dom :as dom :include-macros true]
            [om.core :as om :include-macros true]))

(defn loading []
  (dom/div #js {:className "loading"})
  )

(defn bind-value [cursor path]
  (fn [event]
    (om/transact! cursor path (fn [_] (.. event -target -value)))
    )
  )

(defn text-editor [data owner {:keys [attr className]}]
  (reify
    om/IRender
    (render [_]
      (dom/input #js {:type "text"
                      :value (get data attr)
                      :className className
                      :onChange (bind-value data attr)}))
    ))
