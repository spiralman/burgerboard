(ns burgerboard.server
  (:use ring.adapter.jetty
        korma.db
        burgerboard.handler
        burgerboard.database
        )
  (:require
   [clojure.java.jdbc :as jdbc]
   [taoensso.carmine.ring :as carmine-ring])
  )

(def production-db-spec
  (System/getenv "DATABASE_URL"))

(def production-redis-opts
  {:pool {}
   :spec {:uri (System/getenv "REDISCLOUD_URL")}}
  )

(defdb prod production-db-spec)

(defn init []
  (jdbc/with-connection production-db-spec
    (create-schema)))

(def prod-app
  (build-app (carmine-ring/carmine-store production-redis-opts
                                         {:key-prefix "burgerboard:session"}))
  )
