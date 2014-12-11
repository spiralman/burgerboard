(ns burgerboard.server
  (:use ring.adapter.jetty
        korma.db
        burgerboard.handler
        burgerboard.database
        )
  (:require
   [clojure.java.jdbc :as jdbc])
  )

(defdb prod (System/getenv "DATABASE_URL"))

(defn init []
  (jdbc/with-connection production-db-spec
    (create-schema)))

(def prod-app
  app)
