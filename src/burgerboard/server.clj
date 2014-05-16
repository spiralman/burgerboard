(ns burgerboard.server
  (:use ring.adapter.jetty
        korma.db
        burgerboard.handler
        burgerboard.database
        )
  (:require
   [clojure.java.jdbc :as jdbc])
  )

(def production-db-spec
  (postgres
   {:db "burgerboard"
    :user "burgerboard"
    :password "burgerboard"}))

(defdb prod production-db-spec)

(defn init []
  (jdbc/with-connection production-db-spec
    (create-schema)))

(def prod-app
  app)
