(ns burgerboard.handler
  (:use compojure.core
        burgerboard.database
        burgerboard.users
        )
  (:require [compojure.handler :as handler]
            [compojure.route :as route]))

(defn login [session username password]
  (if (login-valid (find-user username) username password)
    (do
      (assoc session :username "some_user")
      {:status 200
       :session session
       :body "Login"}
      )
    {:status 403
     :body "Invalid username or password"}
    )
  )
  

(defroutes api-routes
  (POST "/login" [username password :as {session :session}]
        (login session username password))
  )

(defroutes app-routes
  (context "/api/v1" [] api-routes)
  (GET "/" [] "Hello World")
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (handler/site app-routes))
