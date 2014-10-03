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
