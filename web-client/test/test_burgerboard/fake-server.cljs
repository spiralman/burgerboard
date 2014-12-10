(ns test-burgerboard.fake-server
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [cemerick.cljs.test :refer [with-test-ctx is]])
  (:require [cemerick.cljs.test :as t]
            [cljs.core.async :refer [<! put! chan]])
  )

(defn body-of [request]
  (cond
   (contains? request :json-data)
   (.stringify js/JSON (clj->js (:json-data request)))
   )
  )

(defn expect-request [test-ctx expected-request response]
  (with-test-ctx test-ctx
    (let [xhr (.useFakeXMLHttpRequest js/sinon)
          expected-url (:url expected-request)
          expected-method (:method expected-request)
          expected-body (body-of expected-request)
          responded (chan)]
      (set! (.-onCreate xhr)
            (fn [request]
              (set! (.-onSend request)
                    (fn [request]
                      (is (= expected-url (.-url request)))
                      (is (= expected-method (.-method request)))
                      (is (= expected-body (.-requestBody request)))
                      (.setTimeout js/window
                                   (fn [_]
                                     (.respond request
                                               (:status response)
                                               (:headers response)
                                               (:body response))
                                     (.setTimeout js/window
                                                  #(put! responded "") 0)
                                     ) 0)
                      )
                    )
              )
            )
      responded
      )
    )
  )

(defn json-response [status body]
  {:status status
   :headers #js {"Content-Type" "application/json"}
   :body (.stringify js/JSON (clj->js body))}
  )
