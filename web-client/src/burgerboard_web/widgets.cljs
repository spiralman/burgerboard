(ns burgerboard-web.widgets
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]])
  (:require [burgerboard-web.api :as api]
            [cljs.core.async :refer [<! put! chan]]
            [om.dom :as dom :include-macros true]
            [om.core :as om :include-macros true]))

(defn loading []
  (dom/div #js {:className "loading"})
  )

(defn loader [data owner {:keys [load-from load-into load-keys]}]
  (reify
    om/IWillMount
    (will-mount [this]
      (go (let [load-keys (or load-keys [])
                response (<! (api/json-get (load-from @data)))]
            (om/transact! data #(assoc % load-into
                                       (get-in response load-keys)))
            ))
      )
    om/IRenderState
    (render-state [this state]
      (loading))
    )
  )

(defn bind-value [owner k]
  (fn -bind-value [event]
    (om/set-state! owner k (.. event -target -value))
    )
  )

(defn text-editor [{:keys [label state-k state-owner className type]}]
  (dom/label #js {:className (str className "-label")}
             label
             (dom/input #js {:type (or type "text")
                             :value (om/get-state state-owner state-k)
                             :className (str className "-input")
                             :onChange (bind-value state-owner state-k)})))

(defn save-single-value [data owner
                         {:keys [className label k url value-saved]}]
  (reify
    om/IInitState
    (init-state [this]
      {:temp-value (k data)
       :save-value (chan)})
    om/IWillMount
    (will-mount [this]
      (go-loop []
               (let [to-save (<! (om/get-state owner :save-value))
                     [post-response post-error] (api/json-post
                                                 url
                                                 {k to-save})]
                 (alt!
                  post-response ([response] (put! value-saved response))
                  post-error (do
                               (om/set-state! owner :error "Save failed")
                               (recur))))))
    om/IRenderState
    (render-state [this state]
      (dom/div #js {:className className}
               (if (contains? (om/get-state owner) :error)
                 (dom/div #js {:className (str className "-error")}
                          (om/get-state owner :error)))
               (text-editor {:state-k :temp-value
                             :state-owner owner
                             :label label
                             :className className})
               (dom/button #js {:className (str className "-save")
                                :type "button"
                                :onClick #(put! (om/get-state owner
                                                              :save-value)
                                                (om/get-state owner
                                                              :temp-value))})
               )
      )
    )
  )
