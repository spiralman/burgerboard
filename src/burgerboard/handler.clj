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

(def app
  (handler/site app-routes))
