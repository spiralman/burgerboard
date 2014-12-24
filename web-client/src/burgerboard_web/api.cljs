(ns burgerboard-web.api
  (:require [cljs.core.async :refer [put! <! chan]]
            [ajax.core :as ajax]))

(defn json-post [url req]
  (let [response (chan)]
    (ajax/POST url
             {:params req
              :format :json
              :response-format :json
              :keywords? true
              :handler (fn [resp] (put! response resp))
              :error-handler (fn [err]
                               (.log js/console (str "got error " err)))}
             )
    response
    )
  )

(defn json-put [url req]
  (let [response (chan)]
    (ajax/PUT url
             {:params req
              :format :json
              :response-format :json
              :keywords? true
              :handler (fn [resp] (put! response resp))
              :error-handler (fn [err]
                               (.log js/console (str "got error " err)))}
             )
    response
    )
  )

(defn json-get [url]
  (let [response (chan)]
    (ajax/GET url
             {:response-format :json
              :keywords? true
              :handler (fn [resp] (put! response resp))
              :error-handler (fn [err]
                               (.log js/console (str "got error " err)))}
             )
    response
    )
  )
