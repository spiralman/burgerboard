(ns burgerboard.handler
  (:use compojure.core)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]))

(defn login [session]
  (assoc session :username "some_user")
  {:status 200
   :session session
   :body "Login"}
  )

(defroutes api-routes
  (POST "/login" {session :session} (login session))
  )

(defroutes app-routes
  (context "/api/v1" [] api-routes)
  (GET "/" [] "Hello World")
  (route/resources "/")
  (route/not-found "Not Found"))

(defn wrap-db [handler db]
  (fn [request]
    (handler (assoc request :db db))
    ))

(defn bind-app [db]
  (->
   (handler/site app-routes)
   (wrap-db db)
   )
  )

(def app
  (bind-app {}))
