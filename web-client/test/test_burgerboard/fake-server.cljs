(ns test-burgerboard.fake-server
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [cemerick.cljs.test :refer [with-test-ctx is]])
  (:require [cljs.core.async :refer [<! put! chan]])
  )

(defn body-of [request]
  (cond
   (contains? request :json-data)
   (.stringify js/JSON (clj->js (:json-data request)))
   )
  )

(defn expect-request [test-ctx expected-request]
  (with-test-ctx test-ctx
    (let [server (.create js/sinon.fakeServer)
          expected-url (:url expected-request)
          expected-method (:method expected-request)
          expected-body (body-of expected-request)
          respond-with (chan)
          responded (chan)]
      (go (let [response (<! respond-with)]
            (.respondWith server expected-method expected-url
                          (fn [request]
                            (is (= expected-url (.-url request)))
                            (is (= expected-method (.-method request)))
                            (is (= expected-body (.-requestBody request)))
                            (.respond request response)
                            (put! responded "")
                            )
                          )
            (.respond server)
            ))
      [respond-with responded]
      )
    )
  )

(defn json-response [status body]
  [status #js {"Content-Type" "application/json"}
   (.stringify js/JSON (clj->js body))]
  )
