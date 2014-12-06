(ns burgerboard.api
  (:use [clojure.string :only [join]]
        [clojure.data.json :as json :only [read-str write-str]]
        )
  )


(defn resolve-route [request & route]
  (join "/"
        (concat
         [(str (-> request :scheme name)
               "://"
               (get-in request [:headers "host"]))
          "api"
          "v1"]
         route)
        )
  )

(defn json-response [data status]
  {:status status
   :body (json/write-str data)}
  )

(defn body-json [request]
  (json/read-str (slurp (:body request))
                 :key-fn keyword)
  )
