(ns burgerboard-web.widgets
  (:require [om.dom :as dom :include-macros true]))

(defn loading []
  (dom/div #js {:className "loading"})
  )
